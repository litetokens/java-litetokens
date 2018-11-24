package stest.tron.wallet.myself;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.WalletGrpc;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Account;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.WalletClient;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class ContinueFreezeUnfreezeforEnergy {
  private AtomicLong count = new AtomicLong();
  private AtomicLong errorCount = new AtomicLong();
  private long startTime = System.currentTimeMillis();

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
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");

  private static final long TotalSupply = 1000000L;

  String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  String url = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetUrl");

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

    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext(true)
        .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);
  }

  public void printAccountResource(byte[] accountAddress, String msg) {
    logger.info("-----------------------------------------");
    if(msg != null) {
      logger.info(msg);
    }

    AccountResourceMessage resourceInfo = PublicMethed.getAccountResource(accountAddress,
        blockingStubFull);
    Account info = PublicMethed.queryAccount(accountAddress, blockingStubFull);
    long balance = info.getBalance();
    long frozenBalanceForEnergy = info.getAccountResource().getFrozenBalanceForEnergy()
        .getFrozenBalance();
    long totalEnergyLimit = resourceInfo.getTotalEnergyLimit();
    long totalEnergyWeight = resourceInfo.getTotalEnergyWeight();
    long energyLimit = resourceInfo.getEnergyLimit();
    long energyUsed = resourceInfo.getEnergyUsed();
    long freeNetLimit = resourceInfo.getFreeNetLimit();
    long netLimit = resourceInfo.getNetLimit();
    long netUsed = resourceInfo.getNetUsed();
    long freeNetUsed = resourceInfo.getFreeNetUsed();

    logger.info("Balance:" + balance);
    logger.info("frozenBalanceForEnergy: " + frozenBalanceForEnergy);
    logger.info("EnergyLimit: " + energyLimit);
    logger.info("EnergyUsed: " + energyUsed);
    logger.info("totalEnergyLimit: " + totalEnergyLimit);
    logger.info("totalEnergyWeight: " + totalEnergyWeight);
    logger.info("FreeNetLimit :" + freeNetLimit);
    logger.info("FreeNetUsed: " + freeNetUsed);
    logger.info("NetLimit: " + netLimit);
    logger.info("NetUsed: " + netUsed);
  }

  private static int randomInt(int min, int max) {
    return (int) Math.round(Math.random()*(max-min)+min);
  }

  @Test(enabled = true, threadPoolSize = 1, invocationCount = 1)
  public void continueRun() {

//    ECKey ecKey1 = new ECKey(Utils.getRandom());
//    byte[] dev001Address = ecKey1.getAddress();
//    String dev001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    // TRxFANjAvztBibiqPRWgG841fVP12BCH7d
    String dev001Key  = "29c91bd8b27c807d8dc2d2991aa0fbeafe7f54f4de9fac1e1684aa57242e3922";
    byte[] dev001Address = new WalletClient(dev001Key).getAddress();

    String store001Key  = "2b85002e80e9bec3bcb4aea175f4a0d33abd9b21b976ba57fa7535e192d6ff7f";
    byte[] store001Address = new WalletClient(store001Key).getAddress();

//    Assert
//        .assertTrue(PublicMethed.sendcoin(dev001Address, 40480000000L, fromAddress,
//            testKey002, blockingStubFull));

    Assert
        .assertTrue(PublicMethed.sendcoin(store001Address, 1000000L, fromAddress,
            testKey002, blockingStubFull));

    for (int ii = 0; ii < 100000000; ii++) {
      count.getAndAdd(4);
      if (count.get() % 100 == 0) {
        long cost = (System.currentTimeMillis() - startTime) / 1000;
        System.out.println("Count:" + count.get() + ", cost:" + cost + ", avg:" + count.get() / cost
            + ", errCount:" + errorCount);
      }

      long freezeBalanceCount = randomInt(1, 1024) * 1000_000L;

      // freeze balance for Energy
      if (!PublicMethed.freezeBalanceGetEnergy(dev001Address, freezeBalanceCount,
          0, 1, dev001Key, blockingStubFull)) {
        errorCount.incrementAndGet();
      }

      // freeze balance for Energy to others
      if (!PublicMethed.freezeBalanceForReceiver(dev001Address, freezeBalanceCount,
          0, 1, ByteString.copyFrom(store001Address), dev001Key, blockingStubFull)) {
        errorCount.incrementAndGet();
      }

      // freeze balance for Net
      if (!PublicMethed.freezeBalance(dev001Address, freezeBalanceCount,
          0, dev001Key, blockingStubFull)) {
        errorCount.incrementAndGet();
      }

      // freeze balance for Net to others
      if (!PublicMethed.freezeBalanceForReceiver(dev001Address, freezeBalanceCount,
          0, 0, ByteString.copyFrom(store001Address), dev001Key, blockingStubFull)) {
        errorCount.incrementAndGet();
      }

      if (count.get() % 10 == 0) {
        printAccountResource(dev001Address, "Before unfreeze, dev001Address:");
        printAccountResource(store001Address, "Before unfreeze, store001Address:");
        if (!PublicMethed.unFreezeBalance(dev001Address, dev001Key, 1, null, blockingStubFull)) {
          errorCount.incrementAndGet();
        }


        if (!PublicMethed.unFreezeBalance(dev001Address, dev001Key, 1, store001Address, blockingStubFull)) {
          errorCount.incrementAndGet();
        }

        if (!PublicMethed.unFreezeBalance(dev001Address, dev001Key, 0, null, blockingStubFull)) {
          errorCount.incrementAndGet();
        }

        if (!PublicMethed.unFreezeBalance(dev001Address, dev001Key, 0, store001Address, blockingStubFull)) {
          errorCount.incrementAndGet();
        }
        printAccountResource(dev001Address, "After unfreeze, dev001Address:");
        printAccountResource(store001Address, "After unfreeze, store001Address:");
      }

        PublicMethed.waitProduceNextBlock(blockingStubFull);
    }
  }

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


