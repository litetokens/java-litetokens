package stest.tron.wallet.contract.originEnergyLimit;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.math.BigInteger;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.SmartContract;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class ContractOriginEnergyLimit035 {


  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

  private ManagedChannel channelFull = null;
  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");

  byte[] contractAddress = null;

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] dev001Address = ecKey1.getAddress();
  String dev001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] user001Address = ecKey2.getAddress();
  String user001Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());


  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
    // get energy
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true).build();
    WalletGrpc.WalletBlockingStub blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    final String testKey001 = Configuration.getByPath("testng.conf")
        .getString("foundationAccount.key1");
    final byte[] freezeAddress = PublicMethed.getFinalAddress(testKey001);
    Assert.assertTrue(PublicMethed.freezeBalanceGetEnergy(freezeAddress, 5000000000000000L,
        0, 1, testKey001, blockingStubFull));
  }

  @AfterSuite
  public void afterSuite() {
    // unfreeze energy
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true).build();
    WalletGrpc.WalletBlockingStub blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    final String testKey001 = Configuration.getByPath("testng.conf")
        .getString("foundationAccount.key1");
    final byte[] freezeAddress = PublicMethed.getFinalAddress(testKey001);
    Assert.assertTrue(PublicMethed.unFreezeBalance(freezeAddress, testKey001, 1,
        null, blockingStubFull));
  }

  @BeforeClass(enabled = true)
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext(true)
        .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);

    Assert.assertTrue(PublicMethed.sendcoin(dev001Address, 1000000L, fromAddress,
        testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(user001Address, 1000000L, fromAddress,
        testKey002, blockingStubFull));
  }

  // Dev and User are same
  public long getAvailableFrozenEnergy(byte[] accountAddress) {
    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(accountAddress,
        blockingStubFull);
    long energyLimit = resourceInfo.getEnergyLimit();
    long energyUsed = resourceInfo.getEnergyUsed();
    return energyLimit - energyUsed;
  }

  public long getUserAvailableEnergy(byte[] userAddress) {
    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(userAddress,
        blockingStubFull);
    Account info = PublicMethed.queryAccount(userAddress, blockingStubFull);
    long balance = info.getBalance();
    long energyLimit = resourceInfo.getEnergyLimit();
    long userAvaliableFrozenEnergy = getAvailableFrozenEnergy(userAddress);
    return balance / 100 + userAvaliableFrozenEnergy;
  }

  public long getFeeLimit(String txid) {
    Optional<Transaction> trsById = PublicMethed.getTransactionById(txid, blockingStubFull);
    return trsById.get().getRawData().getFeeLimit();
  }

  public long getUserMax(byte[] userAddress, long feelimit) {
    Account info = PublicMethed.queryAccount(userAddress, blockingStubFull);
    logger.info("User feeLimit: " + feelimit / 100);
    logger.info("User UserAvaliableEnergy: " + getUserAvailableEnergy(userAddress));
    return Math.min(feelimit / 100, getUserAvailableEnergy(userAddress));
  }

  public long getOriginalEnergyLimit(byte[] contractAddress) {
    SmartContract smartContract = PublicMethed.getContract(contractAddress, blockingStubFull);
    return smartContract.getOriginEnergyLimit();
  }

  public long getConsumeUserResourcePercent(byte[] contractAddress) {
    SmartContract smartContract = PublicMethed.getContract(contractAddress, blockingStubFull);
    return smartContract.getConsumeUserResourcePercent();
  }

  public long getDevMax(byte[] devAddress, byte[] userAddress, long feeLimit,
      byte[] contractAddress) {
    long devMax = Math
        .min(getAvailableFrozenEnergy(devAddress), getOriginalEnergyLimit(contractAddress));
    long p = getConsumeUserResourcePercent(contractAddress);
    if (p != 0) {
      logger.info("p: " + p);
      devMax = Math.min(devMax, getUserMax(userAddress, feeLimit) * (100 - p) / p);
      logger.info("Dev byUserPercent: " + getUserMax(userAddress, feeLimit) * (100 - p) / p);
    }
    logger.info("Dev AvaliableFrozenEnergy: " + getAvailableFrozenEnergy(devAddress));
    logger.info("Dev OriginalEnergyLimit: " + getOriginalEnergyLimit(contractAddress));
    return devMax;
  }

  public static Long getFreezeBalanceCount(byte[] accountAddress, String ecKey, Long targetEnergy,
      WalletGrpc.WalletBlockingStub blockingStubFull, String msg) {
    if (msg != null) {
      logger.info(msg);
    }
    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(accountAddress,
        blockingStubFull);

    Account info = PublicMethed.queryAccount(accountAddress, blockingStubFull);

    Account getAccount = PublicMethed.queryAccount(ecKey, blockingStubFull);

    long balance = info.getBalance();
    long frozenBalance = info.getAccountResource().getFrozenBalanceForEnergy().getFrozenBalance();
    long totalEnergyLimit = resourceInfo.getTotalEnergyLimit();
    long totalEnergyWeight = resourceInfo.getTotalEnergyWeight();
    long energyUsed = resourceInfo.getEnergyUsed();
    long energyLimit = resourceInfo.getEnergyLimit();

    logger.info("Balance:" + balance);
    logger.info("frozenBalance: " + frozenBalance);
    logger.info("totalEnergyLimit: " + totalEnergyLimit);
    logger.info("totalEnergyWeight: " + totalEnergyWeight);
    logger.info("energyUsed: " + energyUsed);
    logger.info("energyLimit: " + energyLimit);

    if (energyUsed > energyLimit) {
      targetEnergy = energyUsed - energyLimit + targetEnergy;
    }

    logger.info("targetEnergy: " + targetEnergy);
    if (totalEnergyWeight == 0) {
      return 1000_000L;
    }

    // totalEnergyLimit / (totalEnergyWeight + needBalance) = needEnergy / needBalance
    BigInteger totalEnergyWeightBI = BigInteger.valueOf(totalEnergyWeight);
    long needBalance = totalEnergyWeightBI.multiply(BigInteger.valueOf(1_000_000))
        .multiply(BigInteger.valueOf(targetEnergy))
        .divide(BigInteger.valueOf(totalEnergyLimit - targetEnergy)).longValue();

    logger.info("[Debug]getFreezeBalanceCount, needBalance: " + needBalance);

    if (needBalance < 1000000L) {
      needBalance = 1000000L;
      logger.info(
          "[Debug]getFreezeBalanceCount, needBalance less than 1 TRX, modify to: " + needBalance);
    }

    return needBalance;
  }


  @Test(enabled = true)
  public void testOriginEnergyLimit() {

    // deploy contract parameters
    long deployFeeLimit = maxFeeLimit;
    long consumeUserResourcePercent = 0;
    long originEnergyLimit = 1000000000;

    //dev balance and Energy
    long devTriggerTargetBalance = 0;
    long devTriggerTargetEnergy = 700;

    // user balance and Energy
    long userTargetBalance = 100000L;
    long userTargetEnergy = 6000L;

    // trigger contract parameter, maxFeeLimit 10000000
    long triggerFeeLimit = 1000000000L;
    boolean expectRet = true;

    // get balance
    Assert.assertTrue(PublicMethed.sendcoin(dev001Address, 10000000, fromAddress,
        testKey002, blockingStubFull));

    AccountResourceMessage accountResource = PublicMethed.getAccountResource(dev001Address,
        blockingStubFull);
    long devEnergyLimitBefore = accountResource.getEnergyLimit();
    long devEnergyUsageBefore = accountResource.getEnergyUsed();
    long devBalanceBefore = PublicMethed.queryAccount(dev001Key, blockingStubFull).getBalance();

    logger.info("before deploy, dev energy limit is " + Long.toString(devEnergyLimitBefore));
    logger.info("before deploy, dev energy usage is " + Long.toString(devEnergyUsageBefore));
    logger.info("before deploy, dev balance is " + Long.toString(devBalanceBefore));

    String contractName = "findArgsByIndex";
    String code = "608060405234801561001057600080fd5b50d3801561001d57600080fd5b50d2801561002a57600080fd5b5060e5806100396000396000f300608060405260043610605c5763ffffffff7c01000000000000000000000000000000000000000000000000000000006000350416632b813bc081146061578063357815c414608d57806350bff6bf14608d578063a26388bb14608d575b600080fd5b348015606c57600080fd5b50d38015607857600080fd5b50d28015608457600080fd5b50608b60b7565b005b348015609857600080fd5b50d3801560a457600080fd5b50d2801560b057600080fd5b50608b605c565bfe00a165627a7a72305820ae9b6cf21dd5cbcb4007002b7cae549b1e0ee6a310c6a52e512e71e4848f6efe0029";
    String abi = "[{\"constant\":false,\"inputs\":[],\"name\":\"testAssert\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"testRequire\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"testThrow\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":false,\"inputs\":[],\"name\":\"testRevert\",\"outputs\":[],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";

    String deployTxid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "",
            deployFeeLimit, 0L, 0, originEnergyLimit, "0",
            0, null, dev001Key, dev001Address, blockingStubFull);

    accountResource = PublicMethed.getAccountResource(dev001Address, blockingStubFull);
    long devEnergyLimitAfter = accountResource.getEnergyLimit();
    long devEnergyUsageAfter = accountResource.getEnergyUsed();
    long devBalanceAfter = PublicMethed.queryAccount(dev001Key, blockingStubFull).getBalance();

    logger.info("after deploy, dev energy limit is " + Long.toString(devEnergyLimitAfter));
    logger.info("after deploy, dev energy usage is " + Long.toString(devEnergyUsageAfter));
    logger.info("after deploy, dev balance is " + Long.toString(devBalanceAfter));

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(deployTxid, blockingStubFull);

    ByteString contractAddressString = infoById.get().getContractAddress();
    contractAddress = contractAddressString.toByteArray();
    SmartContract smartContract = PublicMethed.getContract(contractAddress, blockingStubFull);

    Assert.assertTrue(smartContract.getAbi() != null);

    // count Trigger dev energy, balance
    Long devFreezeBalanceSUN = getFreezeBalanceCount(dev001Address, dev001Key,
        devTriggerTargetEnergy, blockingStubFull, null);

    Long devNeedBalance = devTriggerTargetBalance + devFreezeBalanceSUN;
    logger.info("dev need  balance:" + devNeedBalance);

    // count Trigger user energy, balance
    long userFreezeBalanceSUN = getFreezeBalanceCount(user001Address, user001Key,
        userTargetEnergy, blockingStubFull, null);

    long userNeedBalance = userTargetBalance + userFreezeBalanceSUN;

    logger.info("User need  balance:" + userNeedBalance);

    // get balance
    Assert.assertTrue(PublicMethed.sendcoin(dev001Address, devNeedBalance, fromAddress,
        testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(user001Address, userNeedBalance, fromAddress,
        testKey002, blockingStubFull));

    // get energy
    Assert.assertTrue(PublicMethed.freezeBalanceGetEnergy(dev001Address, devFreezeBalanceSUN,
        3, 1, dev001Key, blockingStubFull));
    Assert.assertTrue(PublicMethed.freezeBalanceGetEnergy(user001Address, userFreezeBalanceSUN,
        3, 1, user001Key, blockingStubFull));

    accountResource = PublicMethed.getAccountResource(dev001Address, blockingStubFull);
    devEnergyLimitBefore = accountResource.getEnergyLimit();
    devEnergyUsageBefore = accountResource.getEnergyUsed();
    devBalanceBefore = PublicMethed.queryAccount(dev001Key, blockingStubFull).getBalance();

    logger
        .info("before trigger, dev devEnergyLimitBefore is " + Long.toString(devEnergyLimitBefore));
    logger
        .info("before trigger, dev devEnergyUsageBefore is " + Long.toString(devEnergyUsageBefore));
    logger.info("before trigger, dev devBalanceBefore is " + Long.toString(devBalanceBefore));

    accountResource = PublicMethed.getAccountResource(user001Address, blockingStubFull);
    long userEnergyLimitBefore = accountResource.getEnergyLimit();
    long userEnergyUsageBefore = accountResource.getEnergyUsed();
    long userBalanceBefore = PublicMethed.queryAccount(user001Address, blockingStubFull)
        .getBalance();

    logger.info(
        "before trigger, user userEnergyLimitBefore is " + Long.toString(userEnergyLimitBefore));
    logger.info(
        "before trigger, user userEnergyUsageBefore is " + Long.toString(userEnergyUsageBefore));
    logger.info("before trigger, user userBalanceBefore is " + Long.toString(userBalanceBefore));

    logger.info("==================================");
    long userMax = getUserMax(user001Address, triggerFeeLimit);
    long devMax = getDevMax(dev001Address, user001Address, triggerFeeLimit, contractAddress);

    logger.info("userMax: " + userMax);
    logger.info("devMax: " + devMax);
    logger.info("==================================");

    String triggerTxid = PublicMethed
        .triggerContract(contractAddress, "testAssert()",
            "#", false, 0, triggerFeeLimit, user001Address, user001Key, blockingStubFull);

    accountResource = PublicMethed.getAccountResource(dev001Address, blockingStubFull);
    devEnergyLimitAfter = accountResource.getEnergyLimit();
    devEnergyUsageAfter = accountResource.getEnergyUsed();
    devBalanceAfter = PublicMethed.queryAccount(dev001Key, blockingStubFull).getBalance();

    logger.info("after trigger, dev devEnergyLimitAfter is " + Long.toString(devEnergyLimitAfter));
    logger.info("after trigger, dev devEnergyUsageAfter is " + Long.toString(devEnergyUsageAfter));
    logger.info("after trigger, dev devBalanceAfter is " + Long.toString(devBalanceAfter));

    accountResource = PublicMethed.getAccountResource(user001Address, blockingStubFull);
    long userEnergyLimitAfter = accountResource.getEnergyLimit();
    long userEnergyUsageAfter = accountResource.getEnergyUsed();
    long userBalanceAfter = PublicMethed.queryAccount(user001Address, blockingStubFull)
        .getBalance();

    logger
        .info("after trigger, user userEnergyLimitAfter is " + Long.toString(userEnergyLimitAfter));
    logger
        .info("after trigger, user userEnergyUsageAfter is " + Long.toString(userEnergyUsageAfter));
    logger.info("after trigger, user userBalanceAfter is " + Long.toString(userBalanceAfter));

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    infoById = PublicMethed.getTransactionInfoById(triggerTxid, blockingStubFull);

    boolean isSuccess = true;
    if (infoById.get().getResultValue() != 0) {
      logger.info("transaction failed with message: " + infoById.get().getResMessage());
      isSuccess = false;
    }
    Assert.assertTrue(infoById.get().getResultValue() == 1);
    long energyTotalUsage = infoById.get().getReceipt().getEnergyUsageTotal();
    logger.info("trigger energyTotalUsage: " + Long.toString(energyTotalUsage));

    smartContract = PublicMethed.getContract(contractAddress, blockingStubFull);
    long consumeURPercent = smartContract.getConsumeUserResourcePercent();
    logger.info("ConsumeURPercent: " + consumeURPercent);

    long devExpectCost = energyTotalUsage * (100 - consumeURPercent) / 100;
    long userExpectCost = energyTotalUsage - devExpectCost;
    long totalCost = devExpectCost + userExpectCost;

    logger.info("devExpectCost: " + devExpectCost);
    logger.info("userExpectCost: " + userExpectCost);

    Assert.assertTrue(devEnergyLimitAfter > 0);
    Assert.assertEquals(devBalanceBefore, devBalanceAfter);

    Assert.assertEquals(devEnergyUsageAfter, devEnergyUsageBefore + devMax);
    logger.info(" new userExpectCost: " + userExpectCost);
    long originEnergyUsageAutual = infoById.get().getReceipt().getOriginEnergyUsage();
    logger.info("originEnergyUsageAutual:" + originEnergyUsageAutual);
//    Assert.assertEquals(originEnergyUsageAutual, originEnergyLimit);
    long FeeAutual = infoById.get().getFee();
//    long EnergyFeeAutual = infoById.get().getReceipt().getEnergyFee();
//    Assert.assertEquals(EnergyUsageAutual * 100 + EnergyFeeAutual, triggerFeeLimit);
//    Assert.assertTrue(userBalanceBefore - EnergyFeeAutual == userBalanceAfter);
    Assert.assertEquals(devEnergyUsageAfter, devEnergyLimitBefore);
//    Assert.assertEquals(userEnergyUsageAfter, userTargetBalance / 100+);

    Assert.assertEquals(userEnergyUsageAfter, userEnergyLimitBefore);
    Assert.assertEquals(userBalanceBefore - FeeAutual, userBalanceAfter);

//    long userTargetBalance = 100000L;
//    long userTargetEnergy = 6000L;
  }

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


