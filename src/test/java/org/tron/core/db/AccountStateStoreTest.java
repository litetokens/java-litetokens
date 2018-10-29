package org.tron.core.db;

import com.google.protobuf.ByteString;
import java.io.File;
import java.util.Objects;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.BytesCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.protos.Contract.TransferContract;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;

public class AccountStateStoreTest {

  private static String dbPath = "output_AccountStateStore_test";
  private static String dbDirectory = "db_AccountStateStore_test";
  private static String indexDirectory = "index_AccountStateStore_test";
  private static TronApplicationContext context;
  private static Manager dbManager;
  private static AccountStateStore accountStateStore;
  private static BlockStore blockStore;
  private static final byte[] key = ByteArray.fromHexString("1111");
  private static final BytesCapsule value = new BytesCapsule(ByteArray.fromHexString("1111"));

  static {
    Args.setParam(
        new String[]{
            "--output-directory", dbPath,
            "--storage-db-directory", dbDirectory,
            "--storage-index-directory", indexDirectory
        },
        Constant.TEST_CONF
    );
  }

  /**
   * Delete database directory, clear args, destroy context.
   */
  @AfterClass
  public static void destroy() {
    Args.clearParam();
    context.destroy();
    FileUtil.deleteDir(new File(dbPath));
  }

  /**
   * Init Spring context, database manager.
   */
  @BeforeClass
  public static void init() {
    context = new TronApplicationContext(DefaultConfig.class);
    dbManager = context.getBean(Manager.class);

    accountStateStore = dbManager.getAccountStateStore();
    blockStore = dbManager.getBlockStore();
    accountStateStore.put(key, value);

  }

  @Test
  public void testGet() {
    //test get and has Method
    Assert.assertEquals(
        ByteArray.toHexString(key),
        ByteArray.toHexString(accountStateStore.get(key).getData())
    );
    Assert.assertTrue(accountStateStore.has(key));
  }

  @Test
  public void testGetById() {
    BlockCapsule blockCapsule = new BlockCapsule(
        1,
        Sha256Hash.wrap(ByteString.copyFrom(
            ByteArray.fromHexString(
                "9938a342238077182498b464ac0292229938a342238077182498b464ac029222"))),
        0,
        ByteString.copyFrom(
            ECKey.fromPrivate(
                ByteArray.fromHexString(
                    Args.getInstance().getLocalWitnesses().getPrivateKey()))
                .getAddress()));

    Sha256Hash accountStateMerkleRoot = blockCapsule.calcAccountStateMerkleRoot(dbManager);

    blockStore.put(blockCapsule.getBlockId().getBytes(), blockCapsule);
    accountStateStore.put(
        blockCapsule.getBlockId().getBytes(),
        new BytesCapsule(accountStateMerkleRoot.getBytes())
    );

    Assert.assertEquals(
        ByteArray.toHexString(Sha256Hash.ZERO_HASH.getBytes()),
        ByteArray.toHexString(
            accountStateStore.getById(
                ByteString.copyFrom(blockCapsule.getBlockId().getBytes())
            ).getData()
        )
    );
  }
}