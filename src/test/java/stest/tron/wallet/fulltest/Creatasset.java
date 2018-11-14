package stest.tron.wallet.fulltest;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.spongycastle.util.encoders.Hex;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.Account;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class Creatasset {


  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);

  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("mainWitness.key2");
  private final byte[] testAddress003 = PublicMethed.getFinalAddress(testKey003);

  private static final String tooLongDescription =
      "1qazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqa"
          + "zxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvq"
          + "azxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcxswedcv";
  private static final String tooLongUrl =
      "qaswqaswqaswqaswqaswqaswqaswqaswqaswqaswqaswqaswqaswqasw1qazxswedcvqazxswedcv"
          + "qazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedc"
          + "vqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqazxswedcvqaz"
          + "xswedcvqazxswedcvqazxswedcwedcv";


  private static long now = System.currentTimeMillis();
  private static String name = "c_" + Long.toString(now);
  long totalSupply = now;
  private static final long sendAmount = 1025000000L;
  private static final long netCostMeasure = 200L;

  Long freeAssetNetLimit = 30000L;
  Long publicFreeAssetNetLimit = 30000L;
  String description = "f";
  String url = "h";


  private ManagedChannel channelFull = null;
//  private WalletGrpc.WalletBlockingStub blockingStubFull = null;

  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);

  //get account
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] asset016Address = ecKey1.getAddress();
  String testKeyForAssetIssue016 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());


  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] transferAssetAddress = ecKey2.getAddress();
  String transferAssetCreateKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(testKey002);
//    channelFull = ManagedChannelBuilder.forTarget(fullnode)
//        .usePlaintext(true)
//        .build();
    //blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
  }

  //@Test(enabled = false)
  @Test(enabled = true, threadPoolSize = 1, invocationCount = 1)
  public void createAssetissue() throws InterruptedException {
    ManagedChannel channelFull1 = ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true)
        .build();;
    WalletGrpc.WalletBlockingStub blockingStubFull = WalletGrpc.newBlockingStub(channelFull1);
    Long start = System.currentTimeMillis() + 2000;
    Long end = System.currentTimeMillis() + 1000000000;
    Account createInfo = PublicMethed.queryAccount(testKey002, blockingStubFull);
    Random rand = new Random();
    Integer randNum;
    int ii=0;
    while (createInfo.getBalance() >= 1025000000&& ii<100000) {
      ECKey ecKey1 = new ECKey(Utils.getRandom());
      byte[] asset016Address = ecKey1.getAddress();
      String testKeyForAssetIssue016 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
      PublicMethed.sendcoin(asset016Address, 1024000000, fromAddress, testKey002, blockingStubFull);
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      start = System.currentTimeMillis() + 20000;
      end = System.currentTimeMillis() + 1000000000;
      randNum = rand.nextInt(300000002);
      name = "taihao_" +  Long.toString(randNum) + "_" + Thread.currentThread().getId();
      logger.info(name);
      PublicMethed.createAssetIssue(asset016Address, name, totalSupply, 1, 1,
          start, end, 1, description, url, freeAssetNetLimit, publicFreeAssetNetLimit,
          10L, 10L, testKeyForAssetIssue016, blockingStubFull);
      try {
        Thread.sleep(3000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      PublicMethed.transferAsset(Hex.decode("411D307314E725336D87CAF65E0C0CFA3449653F8B"), name.getBytes(), 1000L, asset016Address,
          testKeyForAssetIssue016, blockingStubFull);
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      createInfo = PublicMethed.queryAccount(testKey002, blockingStubFull);
      ii++;
    }


  }


  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}