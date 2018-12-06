package org.tron.program;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.util.JSON;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.spongycastle.util.encoders.Hex;
import org.tron.abi.EventEncoder;
import org.tron.abi.FunctionReturnDecoder;
import org.tron.abi.TypeReference;
import org.tron.abi.datatypes.BytesType;
import org.tron.abi.datatypes.Event;
import org.tron.abi.datatypes.Type;
import org.tron.abi.datatypes.generated.AbiTypes;
import org.tron.common.application.Application;
import org.tron.common.storage.leveldb.LevelDbDataSourceImpl;
import org.tron.common.utils.ByteArray;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.TransactionInfoCapsule;
import org.tron.core.config.args.Args;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.SmartContract;
import org.tron.protos.Protocol.SmartContract.ABI;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.Protocol.TransactionInfo.Log;

@Slf4j
public class EventExport {

  private static Cache<String, ABI> abiCache = CacheBuilder.newBuilder()
      .maximumSize(100_000).expireAfterWrite(1, TimeUnit.HOURS).initialCapacity(100_000)
      .recordStats().build();

  public static void main(String[] args) throws InterruptedException {
    logger.info("Solidity node running.");
    Args.setParam(args, Constant.TESTNET_CONF);

    if (true) {
      System.out.println("Start");
      System.out.println(System.currentTimeMillis());
      MongoClient mongoClient = new MongoClient("localhost", 27017);
      MongoDatabase mongoDatabase = mongoClient.getDatabase("EventLogCenter");
      MongoCollection<DBObject> collection = mongoDatabase
          .getCollection("eventLog", DBObject.class);

      LevelDbDataSourceImpl dataSource = new LevelDbDataSourceImpl(
          Args.getInstance().getOutputDirectory(), "transactionHistoryStore");

      dataSource.initDB();

      EventExport eventExport = new EventExport();
      eventExport.initAllContract();

      AtomicLong latestBlockNumber = new AtomicLong(0);
      AtomicLong count = new AtomicLong(0);


      dataSource.allKeys().parallelStream().forEach(key -> {
        byte[] item = dataSource.getData(key);
        TransactionInfo transactionInfo = null;
        try {
          transactionInfo = TransactionInfo.parseFrom(item).toBuilder().build();
          if(transactionInfo.getBlockNumber() > latestBlockNumber.get()) {
            latestBlockNumber.set(transactionInfo.getBlockNumber());
          }
          eventExport.sendEventLog(
              collection,
              transactionInfo.getContractAddress().toByteArray(),
              transactionInfo.getLogList(),
              transactionInfo.getBlockNumber(),
              transactionInfo.getBlockTimeStamp(),
              new TransactionInfoCapsule(transactionInfo)
          );

          if(count.getAndIncrement() % 10000 == 0) {
            System.out.println(count.get());
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      });

      System.out.println("End");
      System.out.println(System.currentTimeMillis());
      System.out.println("latestBlockNumber：" + latestBlockNumber.get());
    }
  }

  private void initAllContract() {
    LevelDbDataSourceImpl dataSource = new LevelDbDataSourceImpl(
        Args.getInstance().getOutputDirectory(), "contract");

    dataSource.initDB();
    dataSource.allValues().iterator().forEachRemaining(item -> {
      SmartContract contract = null;
      try {
        contract = SmartContract.parseFrom(item).toBuilder().build();
        abiCache.put(Wallet.encode58Check(contract.getContractAddress().toByteArray()),
            contract.getAbi());
        //System.out.println(Wallet.encode58Check(contract.getContractAddress().toByteArray()));
      } catch (Exception e) {
        e.printStackTrace();
      }
    });

  }

  private void sendEventLog(MongoCollection<DBObject> collection,
      byte[] contractAddress,
      List<Log> logList, long blockNumber, long blockTimestamp,
      TransactionInfoCapsule transactionInfoCapsule) {
    try {
      Protocol.SmartContract.ABI abi = abiCache.getIfPresent(Wallet.encode58Check(contractAddress));
      if (abi == null) {
        return;
      }
      Protocol.SmartContract.ABI finalAbi = abi;

      IntStream.range(0, logList.size()).forEach(idx -> {
        org.tron.protos.Protocol.TransactionInfo.Log log = logList.get(idx);
        finalAbi.getEntrysList().forEach(abiEntry -> {
          if (abiEntry.getType() != Protocol.SmartContract.ABI.Entry.EntryType.Event) {
            return;
          }
          //parse abi
          String entryName = abiEntry.getName();
          List<TypeReference<?>> typeList = new ArrayList<>();
          List<String> nameList = new ArrayList<>();
          abiEntry.getInputsList().forEach(input -> {
            try {
              TypeReference<?> tr = AbiTypes.getTypeReference(input.getType(), input.getIndexed());
              nameList.add(input.getName());
              typeList.add(tr);
            } catch (UnsupportedOperationException e) {
              logger.error("Unable parse abi entry. {}", e.getMessage());
            }
          });
          JSONObject resultParamType = new JSONObject();
          JSONObject resultJsonObject = new JSONObject();
          JSONObject rawJsonObject = new JSONObject();

          String eventHexString = Hex.toHexString(log.getTopicsList().get(0).toByteArray());
          JSONArray rawTopicsJsonArray = new JSONArray();
          rawTopicsJsonArray.add(eventHexString);

          Event event = new Event(entryName, typeList);
          if (!StringUtils.equalsIgnoreCase(EventEncoder.encode(event), eventHexString)) {
            return;
          }

          String rawLogData = ByteArray.toHexString(log.getData().toByteArray());
          List<Type> nonIndexedValues = FunctionReturnDecoder
              .decode(rawLogData, event.getNonIndexedParameters());

          List<Type> indexedValues = new ArrayList<>();

          List<TypeReference<Type>> indexedParameters = event.getIndexedParameters();
          for (int i = 0; i < indexedParameters.size(); i++) {
            String topicHexString = Hex.toHexString(log.getTopicsList().get(i + 1).toByteArray());
            rawTopicsJsonArray.add(topicHexString);
            Type value = FunctionReturnDecoder
                .decodeIndexedValue(topicHexString, indexedParameters.get(i));
            indexedValues.add(value);
          }
          int counter = 0;
          int indexedCounter = 0;
          int nonIndexedCounter = 0;
          for (TypeReference<?> typeReference : typeList) {

            if (typeReference.isIndexed()) {
              resultJsonObject.put(nameList.get(counter),
                  (indexedValues.get(indexedCounter) instanceof BytesType)
                      ? Hex.toHexString((byte[]) indexedValues.get(indexedCounter).getValue())
                      : indexedValues.get(indexedCounter).getValue().toString());
              String[] abiTypearr = event.getIndexedParameters().get(indexedCounter).getType()
                  .toString().split("\\.");
              resultParamType
                  .put(nameList.get(counter), abiTypearr[abiTypearr.length - 1].toLowerCase());
              indexedCounter++;

            } else {

              String[] abiTypearr = event.getNonIndexedParameters().get(nonIndexedCounter).getType()
                  .toString().split("\\.");
              resultParamType
                  .put(nameList.get(counter), abiTypearr[abiTypearr.length - 1].toLowerCase());
              resultJsonObject.put(nameList.get(counter),
                  (nonIndexedValues.get(nonIndexedCounter) instanceof BytesType)
                      ? Hex.toHexString((byte[]) nonIndexedValues.get(nonIndexedCounter).getValue())
                      : nonIndexedValues.get(nonIndexedCounter).getValue().toString());
              nonIndexedCounter++;
            }
            counter++;
          }

          rawJsonObject.put("topics", rawTopicsJsonArray);
          rawJsonObject.put("data", rawLogData);

//          EventLogEntity eventLogEntity = new EventLogEntity(blockNumber, blockTimestamp,
//              Wallet.encode58Check(contractAddress), entryName, resultJsonObject, rawJsonObject,
//              Hex.toHexString(transactionInfoCapsule.getId()), resultParamType, "FullNode", idx);
//           事件日志写入MongoDB
//          eventLogService.insertEventLog(eventLogEntity);
//          System.out.println(eventLogEntity);

          JSONObject jsonObject = new JSONObject();
          jsonObject.put("_id",
              "FullNode-" + Hex.toHexString(transactionInfoCapsule.getId()) + "-" + idx);
          jsonObject.put("block_number", blockNumber);
          jsonObject.put("block_timestamp", blockTimestamp);
          jsonObject.put("contract_address", Wallet.encode58Check(contractAddress));
          jsonObject.put("event_index", idx);
          jsonObject.put("event_name", entryName);
          jsonObject.put("result", resultJsonObject);
          jsonObject.put("raw", rawJsonObject);
          jsonObject.put("transaction_id", Hex.toHexString(transactionInfoCapsule.getId()));
          jsonObject.put("result_type", resultParamType);
          jsonObject.put("resource_node", "FullNode");

          DBObject dbObject = (DBObject) JSON.parse(jsonObject.toJSONString());
          collection.insertOne(dbObject);
        });
      });
    } catch (Exception e) {
      logger.error("sendEventLog Failed {}", e);
    }
  }


  public static void shutdown(final Application app) {
    logger.info("********register application shutdown hook********");
    Runtime.getRuntime().addShutdownHook(new Thread(app::shutdown));
  }
}