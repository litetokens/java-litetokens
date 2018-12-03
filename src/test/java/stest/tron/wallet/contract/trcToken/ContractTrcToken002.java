package stest.tron.wallet.contract.trcToken;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.math.BigInteger;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.GrpcAPI.AssetIssueList;
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
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.myself.DebugUtils;


@Slf4j
public class ContractTrcToken002 {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");

  private static final long now = System.currentTimeMillis();
  private static String tokenName = "testAssetIssue_" + Long.toString(now);
  private static ByteString assetAccountId = null;
  private static final long TotalSupply = 1000L;
  private byte[] transferTokenContractAddress = null;
  private byte[] receiveTokenContractAddress = null;
  private byte[] tokenBalanceContractAddress = null;

  private String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  private String url = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetUrl");

  private ECKey ecKey1 = new ECKey(Utils.getRandom());
  private byte[] dev001Address = ecKey1.getAddress();
  private String dev001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  private ECKey ecKey2 = new ECKey(Utils.getRandom());
  private byte[] user001Address = ecKey2.getAddress();
  private String user001Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  
  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  @BeforeClass(enabled = true)
  public void beforeClass() {

    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    DebugUtils.printAccountResource(fromAddress, blockingStubFull);
    PublicMethed.printAddress(dev001Key);
    PublicMethed.printAddress(user001Key);

    Assert.assertTrue(PublicMethed.sendcoin(dev001Address, 1100_000_000L, fromAddress,
        testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(user001Address, 100_000_000L, fromAddress,
            testKey002, blockingStubFull));
  }

  public static long getFreezeBalanceCount(byte[] accountAddress, String ecKey, Long targetEnergy,
      WalletGrpc.WalletBlockingStub blockingStubFull, String msg) {
    if(msg != null) {
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
      logger.info("[Debug]getFreezeBalanceCount, needBalance less than 1 TRX, modify to: " + needBalance);
    }
    return needBalance;
  }

  public static Long getAssetIssueValue(byte[] dev001Address, ByteString assetIssueId, WalletGrpc.WalletBlockingStub blockingStubFull) {
    Long assetIssueCount = 0L;
    Account contractAccount = PublicMethed.queryAccount(dev001Address, blockingStubFull);
    Map<String, Long> createAssetIssueMap = contractAccount.getAssetV2Map();
    for (Map.Entry<String, Long> entry : createAssetIssueMap.entrySet()) {
      if (assetIssueId.toStringUtf8().equals(entry.getKey())) {
        assetIssueCount = entry.getValue();
      }
    }
    return assetIssueCount;
  }

  private void testCreateAssetIssue(byte[] accountAddress, String priKey) {
    ByteString addressBS1 = ByteString.copyFrom(accountAddress);
    Account request1 = Account.newBuilder().setAddress(addressBS1).build();
    AssetIssueList assetIssueList1 = blockingStubFull
        .getAssetIssueByAccount(request1);
    Optional<AssetIssueList> queryAssetByAccount = Optional.ofNullable(assetIssueList1);
    if (queryAssetByAccount.get().getAssetIssueCount() == 0) {

      long start = System.currentTimeMillis() + 2000;
      long end = System.currentTimeMillis() + 1000000000;

      //Create a new AssetIssue success.
      Assert.assertTrue(PublicMethed.createAssetIssue(accountAddress, tokenName, TotalSupply, 1,
          10000, start, end, 1, description, url, 100000L,100000L,
          1L,1L, priKey, blockingStubFull));

      Account getAssetIdFromThisAccount = PublicMethed.queryAccount(accountAddress,blockingStubFull);
      assetAccountId = getAssetIdFromThisAccount.getAssetIssuedID();

      logger.info("The token name: " + tokenName);
      logger.info("The token ID: " + assetAccountId.toStringUtf8());

    } else {
      logger.info("This account already create an assetisue");
      Optional<AssetIssueList> queryAssetByAccount1 = Optional.ofNullable(assetIssueList1);
      tokenName = ByteArray.toStr(queryAssetByAccount1.get().getAssetIssue(0).getName().toByteArray());
    }
  }


  @Test
  public void testTrcToken() {
    PublicMethed.printAddress(dev001Key);
    PublicMethed.printAddress(user001Key);

    testCreateAssetIssue(dev001Address, dev001Key);

    logger.info("** deploy transfer token contract");
    deployTransferTokenContract(dev001Address, dev001Key);
    logger.info("** deploy receive token contract");
    deployRevContract(dev001Address, dev001Key);
    logger.info("** trigger transfer token contract to contract address");
    triggerContract(transferTokenContractAddress, receiveTokenContractAddress, user001Address,
        user001Key);
    logger.info("** trigger transfer token contract to normal address");
    triggerContract(transferTokenContractAddress, dev001Address, user001Address, user001Key);
  }

  public void deployTransferTokenContract(byte[] dev001Address, String dev001Key) {
    Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(fromAddress,
        getFreezeBalanceCount(dev001Address, dev001Key, 50000L,
            blockingStubFull, null), 0, 1,
        ByteString.copyFrom(dev001Address), testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(fromAddress, 10_000_000L,
        0, 0, ByteString.copyFrom(dev001Address), testKey002, blockingStubFull));

    testCreateAssetIssue(dev001Address, dev001Key);

    //before deploy, check account resource
    AccountResourceMessage accountResource = PublicMethed.getAccountResource(dev001Address,
        blockingStubFull);
    long energyLimit = accountResource.getEnergyLimit();
    long energyUsage = accountResource.getEnergyUsed();
    long balanceBefore = PublicMethed.queryAccount(dev001Key, blockingStubFull).getBalance();
    Long devAssetCountBefore = getAssetIssueValue(dev001Address, assetAccountId, blockingStubFull);

    logger.info("before energyLimit is " + Long.toString(energyLimit));
    logger.info("before energyUsage is " + Long.toString(energyUsage));
    logger.info("before balanceBefore is " + Long.toString(balanceBefore));
    logger.info("before AssetId: " + assetAccountId.toStringUtf8() +
        ", devAssetCountBefore: " + devAssetCountBefore);

    DebugUtils.printAccountResource(dev001Address, blockingStubFull);

    String contractName = "transferTokenContract";
    String code = "608060405260e2806100126000396000f300608060405260043610603e5763ffffffff7c01000000"
        + "000000000000000000000000000000000000000000000000006000350416633be9ece781146043575b600080"
        + "fd5b606873ffffffffffffffffffffffffffffffffffffffff60043516602435604435606a565b005b604051"
        + "73ffffffffffffffffffffffffffffffffffffffff84169082156108fc029083908590600081818185878a8a"
        + "d094505050505015801560b0573d6000803e3d6000fd5b505050505600a165627a7a723058200ba246bdb58b"
        + "e0f221ad07e1b19de843ab541150b329ddd01558c2f1cefe1e270029";
    String abi = "[{\"constant\":false,\"inputs\":[{\"name\":\"toAddress\",\"type\":\"address\"},"
        + "{\"name\":\"id\",\"type\":\"trcToken\"},{\"name\":\"amount\",\"type\":\"uint256\"}],"
        + "\"name\":\"TransferTokenTo\",\"outputs\":[],\"payable\":true,\"stateMutability\":"
        + "\"payable\",\"type\":\"function\"},{\"inputs\":[],\"payable\":true,\"stateMutability\":"
        + "\"payable\",\"type\":\"constructor\"}]";

    String transferTokenTxid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "",
            maxFeeLimit, 0L, 0, 10000,
            assetAccountId.toStringUtf8(), 100, null, dev001Key,
            dev001Address, blockingStubFull);

    DebugUtils.printContractInfo(transferTokenTxid.getBytes(), blockingStubFull, null);
    DebugUtils.printAccountResource(dev001Address, blockingStubFull);

    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(transferTokenTxid, blockingStubFull);

    if (infoById.get().getResultValue() != 0) {
      Assert.fail("deploy transaction failed with message: " + infoById.get().getResMessage());
    }

    DebugUtils.printContractTxidInfo(transferTokenTxid, blockingStubFull, null);

    transferTokenContractAddress = infoById.get().getContractAddress().toByteArray();
    SmartContract smartContract = PublicMethed.getContract(transferTokenContractAddress,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());

    PublicMethed.waitProduceNextBlock(blockingStubFull);

    // after deploy, check account resource
    DebugUtils.printAccountResource(dev001Address, blockingStubFull);

    accountResource = PublicMethed.getAccountResource(dev001Address, blockingStubFull);
    energyLimit = accountResource.getEnergyLimit();
    energyUsage = accountResource.getEnergyUsed();
    long balanceAfter = PublicMethed.queryAccount(dev001Key, blockingStubFull).getBalance();
    Long devAssetCountAfter = getAssetIssueValue(dev001Address, assetAccountId, blockingStubFull);

    logger.info("after energyLimit is " + Long.toString(energyLimit));
    logger.info("after energyUsage is " + Long.toString(energyUsage));
    logger.info("after balanceAfter is " + Long.toString(balanceAfter));
    logger.info("after AssetId: " + assetAccountId.toStringUtf8() +
        ", devAssetCountAfter: " + devAssetCountAfter);

    Assert.assertTrue(PublicMethed.transferAsset(transferTokenContractAddress,
        assetAccountId.toByteArray(), 100L, dev001Address, dev001Key, blockingStubFull));
    Long contractAssetCount = DebugUtils
        .getAssetIssueValue(transferTokenContractAddress, assetAccountId, blockingStubFull);
    logger.info("Contract has AssetId: " + assetAccountId.toStringUtf8() + ", Count: " + contractAssetCount);

    Assert.assertTrue(energyLimit > 0);
    Assert.assertTrue(energyUsage > 0);
    Assert.assertEquals(balanceBefore, balanceAfter);
    Assert.assertEquals(Long.valueOf(100), Long.valueOf(devAssetCountBefore - devAssetCountAfter));
    Assert.assertEquals(Long.valueOf(200), contractAssetCount);
 }


  public void deployRevContract(byte[] dev001Address, String dev001Key) {
    Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(fromAddress,
        getFreezeBalanceCount(dev001Address, dev001Key, 50000L,
            blockingStubFull, null), 0, 1,
        ByteString.copyFrom(dev001Address), testKey002, blockingStubFull));

    // before deploy, check account resource
    AccountResourceMessage accountResource = PublicMethed.getAccountResource(dev001Address,
        blockingStubFull);
    long energyLimit = accountResource.getEnergyLimit();
    long energyUsage = accountResource.getEnergyUsed();
    long balanceBefore = PublicMethed.queryAccount(dev001Key, blockingStubFull).getBalance();
    Long devAssetCountBefore = getAssetIssueValue(dev001Address, assetAccountId, blockingStubFull);

    logger.info("before energyLimit is " + Long.toString(energyLimit));
    logger.info("before energyUsage is " + Long.toString(energyUsage));
    logger.info("before balance is " + Long.toString(balanceBefore));
    logger.info("before AssetId: " + assetAccountId.toStringUtf8() +
        ", devAssetCountBefore: " + devAssetCountBefore);

    String contractName = "recieveTokenContract";
    String code = "60806040526000805560c5806100166000396000f30060806040526004361060485763ffffffff7c010000000000000000000000000000000000000000000000000000000060003504166362548c7b8114604a578063890eba68146050575b005b6048608c565b348015605b57600080fd5b50d38015606757600080fd5b50d28015607357600080fd5b50607a6093565b60408051918252519081900360200190f35b6001600055565b600054815600a165627a7a723058204c4f1bb8eca0c4f1678cc7cc1179e03d99da2a980e6792feebe4d55c89c022830029";
    String abi = "[{\"constant\":false,\"inputs\":[],\"name\":\"setFlag\",\"outputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[],\"name\":\"flag\",\"outputs\":[{\"name\":\"\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[],\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"constructor\"},{\"payable\":true,\"stateMutability\":\"payable\",\"type\":\"fallback\"}]";
    String recieveTokenTxid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
        0L, 100, 1000, assetAccountId.toStringUtf8(),
            100, null, dev001Key, dev001Address, blockingStubFull);

    // after deploy, check account resource
    accountResource = PublicMethed.getAccountResource(dev001Address, blockingStubFull);
    energyLimit = accountResource.getEnergyLimit();
    energyUsage = accountResource.getEnergyUsed();
    long balanceAfter = PublicMethed.queryAccount(dev001Key, blockingStubFull).getBalance();
    Long devAssetCountAfter = getAssetIssueValue(dev001Address, assetAccountId, blockingStubFull);

    logger.info("after energyLimit is " + Long.toString(energyLimit));
    logger.info("after energyUsage is " + Long.toString(energyUsage));
    logger.info("after balanceAfter is " + Long.toString(balanceAfter));
    logger.info("after AssetId: " + assetAccountId.toStringUtf8() +
        ", devAssetCountAfter: " + devAssetCountAfter);

    DebugUtils.printAccountResource(dev001Address, blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    DebugUtils.printContractTxidInfo(recieveTokenTxid, blockingStubFull, null);

    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(recieveTokenTxid, blockingStubFull);

    if (infoById.get().getResultValue() != 0) {
      Assert.fail("deploy receive failed with message: " + infoById.get().getResMessage());
    }

    receiveTokenContractAddress = infoById.get().getContractAddress().toByteArray();

    SmartContract smartContract = PublicMethed
        .getContract(receiveTokenContractAddress, blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());

    Long contractAssetCount = DebugUtils
        .getAssetIssueValue(receiveTokenContractAddress, assetAccountId, blockingStubFull);
    logger.info("Contract has AssetId: " + assetAccountId.toStringUtf8() + ", Count: " + contractAssetCount);

    Assert.assertTrue(energyLimit > 0);
    Assert.assertTrue(energyUsage > 0);
    Assert.assertEquals(balanceBefore, balanceAfter);
    Assert.assertEquals(Long.valueOf(100), Long.valueOf(devAssetCountBefore - devAssetCountAfter));
    Assert.assertEquals(Long.valueOf(100), contractAssetCount);
  }

  public void triggerContract(byte[] transferTokenContractAddress,
      byte[] receiveTokenContractAddress, byte[] user001Address, String user001Key) {

    Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(fromAddress,
        getFreezeBalanceCount(user001Address, user001Key, 50000L,
            blockingStubFull, null), 0, 1,
        ByteString.copyFrom(user001Address), testKey002, blockingStubFull));

    Assert.assertTrue(PublicMethed.transferAsset(user001Address,
        assetAccountId.toByteArray(), 10L, dev001Address, dev001Key, blockingStubFull));

    DebugUtils.printAccountResource(user001Address, blockingStubFull);

    AccountResourceMessage accountResource = PublicMethed.getAccountResource(dev001Address,
        blockingStubFull);
    long devEnergyLimitBefore = accountResource.getEnergyLimit();
    long devEnergyUsageBefore = accountResource.getEnergyUsed();
    long devBalanceBefore = PublicMethed.queryAccount(dev001Address, blockingStubFull).getBalance();

    logger.info("before trigger, devEnergyLimitBefore is " + Long.toString(devEnergyLimitBefore));
    logger.info("before trigger, devEnergyUsageBefore is " + Long.toString(devEnergyUsageBefore));
    logger.info("before trigger, devBalanceBefore is " + Long.toString(devBalanceBefore));

    accountResource = PublicMethed.getAccountResource(user001Address, blockingStubFull);
    long userEnergyLimitBefore = accountResource.getEnergyLimit();
    long userEnergyUsageBefore = accountResource.getEnergyUsed();
    long userBalanceBefore = PublicMethed.queryAccount(user001Address, blockingStubFull)
        .getBalance();

    logger.info("before trigger, userEnergyLimitBefore is " + Long.toString(userEnergyLimitBefore));
    logger.info("before trigger, userEnergyUsageBefore is " + Long.toString(userEnergyUsageBefore));
    logger.info("before trigger, userBalanceBefore is " + Long.toString(userBalanceBefore));

    DebugUtils.printAccountResource(dev001Address, blockingStubFull);
    DebugUtils.printAccountResource(user001Address, blockingStubFull);

    Long transferAssetBefore = DebugUtils
        .getAssetIssueValue(transferTokenContractAddress, assetAccountId, blockingStubFull);
    logger.info("before trigger, transferTokenContractAddress has AssetId "
        + assetAccountId.toStringUtf8() + ", Count is " + transferAssetBefore);

    Long receiveAssetBefore = DebugUtils
        .getAssetIssueValue(receiveTokenContractAddress, assetAccountId, blockingStubFull);
    logger.info("before trigger, receiveTokenContractAddress has AssetId "
        + assetAccountId.toStringUtf8() + ", Count is " + receiveAssetBefore);

    String param = "\"" + Base58.encode58Check(receiveTokenContractAddress)
        + "\"," + assetAccountId.toStringUtf8() + ",\"1\"";

    String triggerTxid = PublicMethed.triggerContract(transferTokenContractAddress,
        "TransferTokenTo(address,trcToken,uint256)", param, false, 0,
        1000000000L, assetAccountId.toStringUtf8(), 2, user001Address, user001Key,
        blockingStubFull);

    DebugUtils.printAccountResource(dev001Address, blockingStubFull);
    DebugUtils.printAccountResource(user001Address, blockingStubFull);

    accountResource = PublicMethed.getAccountResource(dev001Address, blockingStubFull);
    long devEnergyLimitAfter = accountResource.getEnergyLimit();
    long devEnergyUsageAfter = accountResource.getEnergyUsed();
    long devBalanceAfter = PublicMethed.queryAccount(dev001Address, blockingStubFull).getBalance();

    logger.info("after trigger, devEnergyLimitAfter is " + Long.toString(devEnergyLimitAfter));
    logger.info("after trigger, devEnergyUsageAfter is " + Long.toString(devEnergyUsageAfter));
    logger.info("after trigger, devBalanceAfter is " + Long.toString(devBalanceAfter));

    accountResource = PublicMethed.getAccountResource(user001Address, blockingStubFull);
    long userEnergyLimitAfter = accountResource.getEnergyLimit();
    long userEnergyUsageAfter = accountResource.getEnergyUsed();
    long userBalanceAfter = PublicMethed.queryAccount(user001Address, blockingStubFull).getBalance();

    logger.info("after trigger, userEnergyLimitAfter is " + Long.toString(userEnergyLimitAfter));
    logger.info("after trigger, userEnergyUsageAfter is " + Long.toString(userEnergyUsageAfter));
    logger.info("after trigger, userBalanceAfter is " + Long.toString(userBalanceAfter));

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    DebugUtils
        .printContractTxidInfo(triggerTxid, blockingStubFull,"Trigger transfer token contract:");

    Optional<Transaction> trsById = PublicMethed.getTransactionById(triggerTxid, blockingStubFull);
    long feeLimit = trsById.get().getRawData().getFeeLimit();

    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    if (infoById.get().getResultValue() != 0) {
      Assert.fail("transaction failed with message: " + infoById.get().getResMessage());
    }

    long energyUsage = infoById.get().getReceipt().getEnergyUsage();
    long energyFee = infoById.get().getReceipt().getEnergyFee();
    long originEnergyUsage = infoById.get().getReceipt().getOriginEnergyUsage();

    SmartContract smartContract = PublicMethed.getContract(infoById.get().getContractAddress()
        .toByteArray(), blockingStubFull);

    Long transferAssetAfter = getAssetIssueValue(transferTokenContractAddress,
        assetAccountId, blockingStubFull);
    logger.info("after trigger, transferTokenContractAddress has AssetId "
        + assetAccountId.toStringUtf8() + ", transferAssetAfter is " + transferAssetAfter);

    Long receiveAssetAfter = getAssetIssueValue(receiveTokenContractAddress,
        assetAccountId, blockingStubFull);
    logger.info("after trigger, receiveTokenContractAddress has AssetId "
        + assetAccountId.toStringUtf8() + ", receiveAssetAfter is " + receiveAssetAfter);

    long consumeURPercent = smartContract.getConsumeUserResourcePercent();
    logger.info("ConsumeURPercent: " + consumeURPercent);

    Assert.assertEquals(originEnergyUsage, devEnergyUsageAfter - devEnergyUsageBefore);
    Assert.assertEquals(energyUsage, userEnergyUsageAfter - userEnergyUsageBefore);
    Assert.assertEquals(energyFee, userBalanceBefore - userBalanceAfter);
    Assert.assertEquals(receiveAssetAfter - receiveAssetBefore, transferAssetBefore + 2L - transferAssetAfter);
  }

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


