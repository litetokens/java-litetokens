package org.tron.common.runtime;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import org.testng.Assert;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.storage.DepositImpl;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.actuator.Actuator;
import org.tron.core.actuator.ActuatorFactory;
import org.tron.core.actuator.AssetIssueActuator;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.AssetIssueCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.capsule.TransactionResultCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.exception.ContractExeException;
import org.tron.core.exception.ContractValidateException;
import org.tron.core.exception.ReceiptCheckErrException;
import org.tron.core.exception.VMIllegalException;
import org.tron.protos.Contract;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Result.code;
import stest.tron.wallet.common.client.utils.DataWord;

@Slf4j
public class TRC10TokenTest {
  private static Runtime runtime;
  private static Manager dbManager;
  private static TronApplicationContext context;
  private static Application appT;
  private static DepositImpl deposit;
  private static final String dbPath = "output_TRC10TokenTest";
  private static final String OWNER_ADDRESS;
  private static final String NAME = "testToken";
  private static final String TRANSFER_TO;
  private static final long TOTAL_SUPPLY = 10000L;
  private static final int TRX_NUM = 10000;
  private static final int NUM = 100000;
  private static final String DESCRIPTION = "myCoin";
  private static final String URL = "tron-my.com";
  private static final String ASSET_NAME_SECOND = "asset_name2";
  private static long now = 0;
  private static long startTime = 0;
  private static long endTime = 0;

  static {
    Args.setParam(new String[]{"--output-directory", dbPath, "--debug"}, Constant.TEST_CONF);
    context = new TronApplicationContext(DefaultConfig.class);
    appT = ApplicationFactory.create(context);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
    TRANSFER_TO = Wallet.getAddressPreFixString() + "548794500882809695a8a687866e76d4271a1abc";

  }
  @BeforeClass
  public static void init() {
    dbManager = context.getBean(Manager.class);
  }

  @Before
  public void create()
      throws IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException {
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(24 * 3600 * 1000);

    now = dbManager.getHeadBlockTimeStamp();
    startTime = now + 48 * 3600 * 1000;
    endTime = now + 72 * 3600 * 1000;
    Class assetIssueClass= Class.forName("AssetIssueActuator");
    Constructor assetIssueConstructor[]=assetIssueClass.getDeclaredConstructors();
    assetIssueConstructor[0].setAccessible(true);
    long nowTime = new Date().getTime();
    Any any = Any.pack(
        Contract.AssetIssueContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ByteArray.fromHexString(OWNER_ADDRESS)))
            .setName(ByteString.copyFromUtf8(NAME))
            .setTotalSupply(TOTAL_SUPPLY)
            .setTrxNum(TRX_NUM)
            .setNum(NUM)
            .setStartTime(nowTime)
            .setEndTime(nowTime + 24 * 3600 * 1000)
            .setDescription(ByteString.copyFromUtf8(DESCRIPTION))
            .setUrl(ByteString.copyFromUtf8(URL))
            .build());
    AssetIssueActuator actuator= (AssetIssueActuator)assetIssueConstructor[0].newInstance(any,dbManager);
    TransactionResultCapsule ret = new TransactionResultCapsule();
    //ActuatorFactory.createActuator(new TransactionCapsule(),dbManager);
    Long blackholeBalance = dbManager.getAccountStore().getBlackhole().getBalance();
    try {
      actuator.validate();
      actuator.execute(ret);
      org.junit.Assert.assertEquals(ret.getInstance().getRet(), code.SUCESS);
      AccountCapsule owner =
          dbManager.getAccountStore().get(ByteArray.fromHexString(OWNER_ADDRESS));
      AssetIssueCapsule assetIssueCapsule =
          dbManager.getAssetIssueStore().get(ByteString.copyFromUtf8(NAME).toByteArray());
      org.junit.Assert.assertNotNull(assetIssueCapsule);

      org.junit.Assert.assertEquals(owner.getBalance(), 0L);
      org.junit.Assert.assertEquals(dbManager.getAccountStore().getBlackhole().getBalance(),
          blackholeBalance + dbManager.getDynamicPropertiesStore().getAssetIssueFee());
      org.junit.Assert.assertEquals(owner.getAssetMap().get(NAME).longValue(), TOTAL_SUPPLY);
    } catch (ContractValidateException e) {
      org.junit.Assert.assertFalse(e instanceof ContractValidateException);
    } catch (ContractExeException e) {
      org.junit.Assert.assertFalse(e instanceof ContractExeException);
    } finally {
      dbManager.getAssetIssueStore().delete(ByteArray.fromString(NAME));
    }
  }

  /**
   * pragma solidity <0.4.24;
   *
   *  contract token{
   *      constructor() public payable{}
   *      function TransferTokenTo(address toAddress) public payable{
   *          bytes32 id = 0x74657374546f6b656e;
   *          toAddress.transferToken(5,id);
   *      }
   *  }
   */

  @Test
  public void tokenTransferWhenDeployContractTest()
      throws ContractExeException, ReceiptCheckErrException, VMIllegalException, ContractValidateException {
    String contractName = "TransferWhenDeployContract";
    byte[] address = Hex.decode(OWNER_ADDRESS);
    String ABI = "[{\"constant\":false,\"inputs\":[{\"name\":\"toAddress\",\"type\":\"address\"}],\"name\":\""
        + "TransferTokenTo\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function"
        + "\"},{\"inputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"constructor\"}]";
    String code = "608060405260e2806100126000396000f300608060405260043610603e5763ffffffff7c0100000000000000000"
        + "0000000000000000000000000000000000000006000350416634ca8bf6a81146043575b600080fd5b606273ffffffffffff"
        + "ffffffffffffffffffffffffffff600435166064565b005b6040516874657374546f6b656e9073fffffffffffffffffffff"
        + "fffffffffffffffffff83169060009060059084908381818185878a84d094505050505015801560b1573d6000803e3d6000"
        + "fd5b5050505600a165627a7a723058205ae40f614266d6da1591005cb05c1b8eeaf2c14cb11784e13491443c7dc82af40029";
    long value = 100;
    long fee = 100000000;
    long consumeUserResourcePercent = 0;
    long originEnergyLimit = 10000000;
    String tokenId = "testToken";

//    Transaction trx = TVMTestUtils.generateDeploySmartContractAndGetTransaction(
//        contractName, address, ABI, code, value, fee, consumeUserResourcePercent, null,originEnergyLimit,1000,tokenId);
//    byte[] contractAddress = Wallet.generateContractAddress(trx);
//    runtime = TVMTestUtils.processTransactionAndReturnRuntime(trx, deposit, null);
//    Assert.assertNull(runtime.getRuntimeError());
//    Assert.assertEquals(deposit.getBalance(contractAddress), 100);
//    Assert.assertEquals(deposit.getTokenBalance(contractAddress, new DataWord(tokenId).getData()), 100);
//    //Assert.assertEquals(deposit.getAccountStore().get(contractAddress).getBalance(), 100);
//    recoverDeposit();

  }

  private void recoverDeposit() {
    dbManager = context.getBean(Manager.class);
    deposit = DepositImpl.createRoot(dbManager);
  }

  /**
   * Release resources.
   */
  @AfterClass
  public static void destroy() {
    Args.clearParam();
    appT.shutdownServices();
    appT.shutdown();
    context.destroy();
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
  }

}
