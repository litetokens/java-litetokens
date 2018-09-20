package org.tron.core.capsule;

import com.google.protobuf.ByteString;
import java.io.File;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
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
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.AccountStore;
import org.tron.core.db.Manager;
import org.tron.core.exception.BadItemException;
import org.tron.protos.Contract.TransferContract;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;

@Slf4j
public class BlockCapsuleTest {

  private static Manager dbManager;
  private static TronApplicationContext context;
  private static BlockCapsule blockCapsule1;
  private static BlockCapsule blockCapsule2;

  private static BlockCapsule blockCapsule0 = new BlockCapsule(1,
      Sha256Hash.wrap(ByteString
          .copyFrom(ByteArray
              .fromHexString("9938a342238077182498b464ac0292229938a342238077182498b464ac029222"))),
      1234,
      ByteString.copyFrom("1234567".getBytes()));
  private static String dbPath = "output_bloackcapsule_test";

  @BeforeClass
  public static void init() {
    Args.setParam(new String[]{"-d", dbPath},
        Constant.TEST_CONF);

    context = new TronApplicationContext(DefaultConfig.class);

    dbManager = context.getBean(Manager.class);

    blockCapsule1 = new BlockCapsule(
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

    blockCapsule2 = new BlockCapsule(
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

    blockCapsule2.addTransaction(
        new TransactionCapsule(
            TransferContract.newBuilder()
                .setAmount(1)
                .setOwnerAddress(ByteString
                    .copyFrom(Objects.requireNonNull(
                        Wallet.decodeFromBase58Check("27QAUYjg5FXfxcvyHcWF3Aokd5eu9iYgs1c"))))
                .setToAddress(ByteString
                    .copyFrom(Objects.requireNonNull(
                        Wallet.decodeFromBase58Check("27g8BKC65R7qsnEr2vf7R2Nm7MQfvuJ7im4"))))
                .build(), ContractType.TransferContract

        ));

    AccountStore accountStore = dbManager.getAccountStore();
    AccountCapsule owner = new AccountCapsule(
        Account.newBuilder()
            .setAddress(
                ByteString.copyFrom(
                    Objects.requireNonNull(
                        Wallet.decodeFromBase58Check("27QAUYjg5FXfxcvyHcWF3Aokd5eu9iYgs1c"))))
            .build());
    AccountCapsule to = new AccountCapsule(
        Account.newBuilder()
            .setAddress(
                ByteString.copyFrom(
                    Objects.requireNonNull(
                        Wallet.decodeFromBase58Check("27g8BKC65R7qsnEr2vf7R2Nm7MQfvuJ7im4"))))
            .build());
    accountStore.put(owner.getAddress().toByteArray(), owner);
    accountStore.put(to.getAddress().toByteArray(), to);
  }

  @AfterClass
  public static void removeDb() {
    Args.clearParam();
    FileUtil.deleteDir(new File(dbPath));
  }

  @Test
  public void testCalcMerkleRoot() throws Exception {
    blockCapsule0.setMerkleRoot();
    Assert.assertEquals(
        Sha256Hash.wrap(Sha256Hash.ZERO_HASH.getByteString()).toString(),
        blockCapsule0.getMerkleRoot().toString());

    logger.info("Transaction[X] Merkle Root : {}", blockCapsule0.getMerkleRoot().toString());

    TransferContract transferContract1 = TransferContract.newBuilder()
        .setAmount(1L)
        .setOwnerAddress(ByteString.copyFrom("0x0000000000000000000".getBytes()))
        .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(
            (Wallet.getAddressPreFixString() + "A389132D6639FBDA4FBC8B659264E6B7C90DB086"))))
        .build();

    TransferContract transferContract2 = TransferContract.newBuilder()
        .setAmount(2L)
        .setOwnerAddress(ByteString.copyFrom("0x0000000000000000000".getBytes()))
        .setToAddress(ByteString.copyFrom(ByteArray.fromHexString(
            (Wallet.getAddressPreFixString() + "ED738B3A0FE390EAA71B768B6D02CDBD18FB207B"))))
        .build();

    blockCapsule0
        .addTransaction(new TransactionCapsule(transferContract1, ContractType.TransferContract));
    blockCapsule0
        .addTransaction(new TransactionCapsule(transferContract2, ContractType.TransferContract));
    blockCapsule0.setMerkleRoot();

    if (Constant.ADD_PRE_FIX_BYTE_TESTNET == Wallet.getAddressPreFixByte()) {
      Assert.assertEquals(
          "53421c1f1bcbbba67a4184cc3dbc1a59f90af7e2b0644dcfc8dc738fe30deffc",
          blockCapsule0.getMerkleRoot().toString());
    } else {
      Assert.assertEquals(
          "5bc862243292e6aa1d5e21a60bb6a673e4c2544709f6363d4a2f85ec29bcfe00",
          blockCapsule0.getMerkleRoot().toString());
    }

    logger.info("Transaction[O] Merkle Root : {}", blockCapsule0.getMerkleRoot().toString());
  }

  /* @Test
  public void testAddTransaction() {
    TransactionCapsule transactionCapsule = new TransactionCapsule("123", 1L);
    blockCapsule0.addTransaction(transactionCapsule);
    Assert.assertArrayEquals(blockCapsule0.getTransactions().get(0).getHash().getBytes(),
        transactionCapsule.getHash().getBytes());
    Assert.assertEquals(transactionCapsule.getInstance().getRawData().getVout(0).getValue(),
        blockCapsule0.getTransactions().get(0).getInstance().getRawData().getVout(0).getValue());
  } */

  @Test
  public void testGetData() {
    blockCapsule0.getData();
    byte[] b = blockCapsule0.getData();
    BlockCapsule blockCapsule1 = null;
    try {
      blockCapsule1 = new BlockCapsule(b);
      Assert.assertEquals(blockCapsule0.getBlockId(), blockCapsule1.getBlockId());
    } catch (BadItemException e) {
      e.printStackTrace();
    }

  }

  @Test
  public void testValidate() {

  }

  @Test
  public void testGetInsHash() {
    Assert.assertEquals(1,
        blockCapsule0.getInstance().getBlockHeader().getRawData().getNumber());
    Assert.assertEquals(blockCapsule0.getParentHash(),
        Sha256Hash.wrap(blockCapsule0.getParentHashStr()));
  }

  @Test
  public void testGetTimeStamp() {
    Assert.assertEquals(1234L, blockCapsule0.getTimeStamp());
  }

  @Test
  public void testCalcAccountStateMerkleRoot() {
    Sha256Hash accountStateMerkleRoot1 = blockCapsule1.calcAccountStateMerkleRoot(dbManager);
    Assert.assertEquals(Sha256Hash.ZERO_HASH, accountStateMerkleRoot1);

    Sha256Hash accountStateMerkleRoot2 = blockCapsule2.calcAccountStateMerkleRoot(dbManager);
    Assert.assertEquals(
        "e3801c38a38527b61e41afbf133de9c61c05b1ede74db0e344605a9709e9d390",
        accountStateMerkleRoot2.toString());
  }

  @Test
  public void testSetAccountStateMerkleRoot() {
    blockCapsule1.setAccountStateMerkleRoot(dbManager);
    Assert.assertEquals(Sha256Hash.ZERO_HASH, blockCapsule1.getAccountStateMerkleRoot());

    blockCapsule2.setAccountStateMerkleRoot(dbManager);
    Assert.assertEquals(
        "e3801c38a38527b61e41afbf133de9c61c05b1ede74db0e344605a9709e9d390",
        blockCapsule2.getAccountStateMerkleRoot().toString());
  }
}