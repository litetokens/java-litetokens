package org.tron.core.db;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Streams;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.CodeCapsule;
import org.tron.core.capsule.ContractCapsule;
import org.tron.protos.Protocol.SmartContract;

@Slf4j
@Component
public class ContractStore extends TronStoreWithRevoking<ContractCapsule> {
  AtomicLong hitCnt = new AtomicLong(0);
  AtomicLong accessCnt = new AtomicLong(0);
  ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();

  {
    service.scheduleWithFixedDelay(() -> {
      try {
        double rate = hitCnt.get() * 1.0 * 100 / accessCnt.get();
        logger.error("contract hitrate:" + " access:" + (long)(rate) + "% hit:" + hitCnt.get() + "");
      } catch (Throwable t) {
        logger.error("Exception in log worker", t);
      }
    }, 10, 10, TimeUnit.SECONDS);
  }
  @Getter
  private Cache<ByteArrayWrapper, ContractCapsule> codeCache = CacheBuilder
      .newBuilder().maximumSize(100_000).recordStats().build();
  private Cache<ByteArrayWrapper, SmartContract.ABI> abiCache = CacheBuilder
      .newBuilder().maximumSize(100_000).recordStats().build();

  @Autowired
  private ContractStore(@Value("contract") String dbName) {
    super(dbName);
  }

  @Override
  public void put(byte[] key, ContractCapsule item) {
    codeCache.put(new ByteArrayWrapper(key), item);
    super.put(key, item);
  }

  @Override
  public ContractCapsule get(byte[] key) {
    ContractCapsule ret = codeCache.getIfPresent(new ByteArrayWrapper(key));
    if (ret == null) {
      ret = getUnchecked(key);
      codeCache.put(new ByteArrayWrapper(key), ret);
    } else {
      hitCnt.incrementAndGet();
    }
    return ret;
  }

  /**
   * get total transaction.
   */
  public long getTotalContracts() {
    return Streams.stream(revokingDB.iterator()).count();
  }

  private static ContractStore instance;

  public static void destory() {
    instance = null;
  }

  void destroy() {
    instance = null;
  }

  /**
   * find a transaction  by it's id.
   */
  public byte[] findContractByHash(byte[] trxHash) {
    return revokingDB.getUnchecked(trxHash);
  }

  /**
   *
   * @param contractAddress
   * @return
   */
  public SmartContract.ABI getABI(byte[] contractAddress) {
    SmartContract.ABI ret = abiCache.getIfPresent(contractAddress);
    if (ret == null) {

      byte[] value = revokingDB.getUnchecked(contractAddress);
      if (ArrayUtils.isEmpty(value)) {
        return null;
      }

      ContractCapsule contractCapsule = new ContractCapsule(value);
      SmartContract smartContract = contractCapsule.getInstance();
      if (smartContract == null) {
        return null;
      }
      ret = smartContract.getAbi();
      abiCache.put(new ByteArrayWrapper(contractAddress), ret);
    }
    return ret;
  }

}
