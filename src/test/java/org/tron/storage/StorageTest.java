package org.tron.storage;

import com.google.protobuf.ByteString;
import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.common.utils.Utils;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.capsule.WitnessCapsule;
import org.tron.core.capsule.utils.BlockUtil;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.common.utils.FileUtil;
import org.tron.core.db.DynamicPropertiesStore;
import org.tron.core.db.Manager;
import org.tron.core.witness.WitnessController;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Transaction;

/**
 * @program: java-tron
 * @description:
 * @author: shydesky@gmail.com
 * @create: 2018-10-16
 **/

public class StorageTest {

  public Application appT;
  public TronApplicationContext context;


  public String dbPath = "output-directory-storage";
  public String database = "database";
  public String index = "index";

  @Test
  public void printGenesisBlock() {
    BlockCapsule block = BlockUtil.newGenesisBlockCapsule();
    System.out.println(block);
    System.out.println("genesisBlock.getNum():" + block.getNum());
    System.out.println("genesisBlock.getBlockId():" + block.getBlockId());
    System.out.println("genesisBlock.getTimeStamp():" + block.getTimeStamp());
    System.out.println("genesisBlock.getParentHash():" + block.getParentHash());
    System.out.println("genesisBlock.getTransactions():" + block.getTransactions());
    Assert.assertEquals(block.getNum(), 0);
  }

  @Test
  public void getBlock() throws Throwable {
    BlockCapsule block = BlockUtil.newGenesisBlockCapsule();
    Manager db = context.getBean(Manager.class);
    //db.pushBlock(block);
    BlockCapsule block_ = db.getBlockByNum(0);
    DynamicPropertiesStore dydb = db.getDynamicPropertiesStore();
    System.out.println(
        "getLatestBlockHeaderHash:" + dydb.getLatestBlockHeaderHash());
    System.out.println("supportVM:" + dydb.supportVM());
    System.out.println("getNextMaintenanceTime:" + dydb.getNextMaintenanceTime());
    System.out.println("getWitnessPayPerBlock:" + dydb.getWitnessPayPerBlock());
  }

  @Test
  public void generateBlock() throws Throwable {
    BlockCapsule gBlock = BlockUtil.newGenesisBlockCapsule();

    Manager db = context.getBean(Manager.class);
    WitnessController witnessController = db.getWitnessController();

   // db.getWitnesses().clear();
    ECKey ecKey = new ECKey(Utils.getRandom());
    String privateKey = ByteArray.toHexString(ecKey.getPrivKey().toByteArray());
    ByteString address = ByteString.copyFrom(ecKey.getAddress());

    WitnessCapsule witnessCapsule = new WitnessCapsule(address);
    db.getWitnessStore().put(address.toByteArray(), witnessCapsule);
    db.getWitnessController().addWitness(address);

    /*AccountCapsule accountCapsule =
        new AccountCapsule(Account.newBuilder().setAddress(address).build());
    db.getAccountStore().put(address.toByteArray(), accountCapsule);
*/
    BlockCapsule newBlock = new BlockCapsule(gBlock.getNum() + 1, (Sha256Hash) gBlock.getBlockId(),
        gBlock.getTimeStamp()+3000, witnessController.getScheduledWitness(witnessController.getSlotAtTime(3000)));
    newBlock.setMerkleRoot();
    newBlock.generatedByMyself = true;
    newBlock.sign(ByteArray.fromHexString(privateKey));

    db.pushBlock(newBlock);


    /*ECKey ecKey2 = new ECKey(Utils.getRandom());
    String privateKey2 = ByteArray.toHexString(ecKey2.getPrivKey().toByteArray());
    ByteString address2 = ByteString.copyFrom(ecKey2.getAddress());

    WitnessCapsule witnessCapsule2 = new WitnessCapsule(address2);
    db.getWitnessStore().put(address2.toByteArray(), witnessCapsule2);
    db.getWitnessController().addWitness(address2);
    */
    /*AccountCapsule accountCapsule2 =
        new AccountCapsule(Account.newBuilder().setAddress(address2).build());
    db.getAccountStore().put(address2.toByteArray(), accountCapsule2);
*/
    //Thread.sleep(3000);
    //addressToProvateKeys =
    BlockCapsule newBlock2 = new BlockCapsule(newBlock.getNum() + 1, (Sha256Hash) newBlock.getBlockId(),
        newBlock.getTimeStamp() + 3000, witnessController.getScheduledWitness(witnessController.getSlotAtTime(6000)));
    newBlock2.setMerkleRoot();
    newBlock2.generatedByMyself = true;
    //newBlock2.sign(ByteArray.fromHexString(ByteArray.fromHexString(addressToProvateKeys.get(witnessAddress)));

    db.pushBlock(newBlock2);

    // validate
    DynamicPropertiesStore dydb = db.getDynamicPropertiesStore();
    System.out.println(dydb.getLatestBlockHeaderTimestamp());
    System.out.println("newBlock.getBlockId():" + newBlock.getBlockId().toString());
    System.out.println(
        "getLatestBlockHeaderHash:" + dydb.getLatestBlockHeaderHash());
  }

  @Test
  public void testGetSlotTime(){
    Manager db = context.getBean(Manager.class);

    long slot = db.getWitnessController().getSlotTime(1);
    System.out.println("slot:" + slot);
  }


  /*@Test
  public void testECKey(){
    String privateKey = "630BBFD143D74BA3D4E39A943F8D73E22C1103B3EB76BC168EAAA91D390ED095";
    BigInteger priK = new BigInteger(privateKey, 16);

    ECKey ecKey = ECKey.fromPrivate(priK);

    ByteString bs = ByteString.copyFrom(ByteArray.fromHexString("0x7B2276657273696F6E223A312C226B6579223A226134346564663338313034343339376536353836316139663835646435663864646135393666303136633836626161363331643463633137626533656366366638393064643331343830363233396137653563343639353031386536393437623430646333313236343661316666376333373037656566323235366265323161222C2261646472657373223A225444514B344B77326150626D5571694C796248586B4B563167485048504B6D50734D222C2273616C74223A2231343534383839612D643836622D343861362D396465332D373030303934656236383934227D"));
    System.out.println("testECKey:" + ByteString.copyFrom(ECKey.computeAddress(bs.toByteArray())));
  }*/

  @Before
  public void init() {
    Args.setParam(
        new String[]{
            "--output-directory", dbPath,
            "--storage-db-directory", database,
            "--storage-index-directory", index
        },
        "config.conf"
    );
    Args cfgArgs = Args.getInstance();
    cfgArgs.setLogLevel("ERROR");
    cfgArgs.setNodeListenPort(17892);
    cfgArgs.setNodeDiscoveryEnable(false);
    cfgArgs.getSeedNode().getIpList().clear();
    cfgArgs.setNeedSyncCheck(false);
    cfgArgs.setNodeExternalIp("127.0.0.1");

    context = new TronApplicationContext(DefaultConfig.class);
    appT = ApplicationFactory.create(context);
  }

  @After
  public void destroy() throws Throwable {
    appT.shutdownServices();
    appT.shutdown();
    context.destroy();
    if (!FileUtil.deleteDir(new File(dbPath))) {
      throw new Exception("release failure!");
    }
  }
}