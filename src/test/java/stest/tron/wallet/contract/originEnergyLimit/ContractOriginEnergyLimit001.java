package stest.tron.wallet.contract.originEnergyLimit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Utils;
import org.tron.core.Wallet;
import org.tron.protos.Protocol.SmartContract;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class ContractOriginEnergyLimit001 {


  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] testNetAccountAddress = PublicMethed.getFinalAddress(testNetAccountKey);
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private ManagedChannel channelSolidity = null;

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;

  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;


  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;

  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);


  byte[] contractAddress = null;

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] grammarAddress3 = ecKey1.getAddress();
  String testKeyForGrammarAddress3 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());


  @BeforeSuite
  public void beforeSuite() {
    Wallet wallet = new Wallet();
    Wallet.setAddressPreFixByte(CommonConstant.ADD_PRE_FIX_BYTE_MAINNET);
  }

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(testKeyForGrammarAddress3);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext(true)
        .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);

    logger.info(Long.toString(PublicMethed.queryAccount(testNetAccountKey, blockingStubFull)
        .getBalance()));

  }

  //Origin_energy_limit001,028,029
  @Test(enabled = true)
  public void testOrigin_energy_limit001() {
    PublicMethed
        .sendcoin(grammarAddress3, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull);
    String contractName = "aContract";
    String code = "608060405234801561001057600080fd5b50d3801561001d57600080fd5b50d2801561002a57600080fd5b5061014e8061003a6000396000f3006080604052600436106100405763ffffffff7c0100000000000000000000000000000000000000000000000000000000600035041663329000b58114610045575b600080fd5b34801561005157600080fd5b50d3801561005e57600080fd5b50d2801561006b57600080fd5b50610077600435610089565b60408051918252519081900360200190f35b604080516003808252608082019092526000916060919060208201838038833901905050905060018160008151811015156100c057fe5b602090810290910101528051600290829060019081106100dc57fe5b602090810290910101528051600390829060029081106100f857fe5b60209081029091010152805181908490811061011057fe5b906020019060200201519150509190505600a165627a7a7230582058dd457e2aeba46e78dd8b9c36b777d362763c05ec1ad62e0d79de51ff3dde790029";
    String abi = "[{\"constant\":false,\"inputs\":[{\"name\":\"i\",\"type\":\"uint256\"}],\"name\":\"findArgsByIndexTest\",\"outputs\":[{\"name\":\"z\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]";
    String contractAddress = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
            0L, 100, -1, "0", 0, null, testKeyForGrammarAddress3,
            grammarAddress3, blockingStubFull);

    Assert.assertTrue(contractAddress == null);

    String contractAddress1 = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
            0L, 100, 0, "0", 0, null, testKeyForGrammarAddress3,
            grammarAddress3, blockingStubFull);

    Assert.assertTrue(contractAddress1 == null);

    byte[] contractAddress2 = PublicMethed
        .deployContract(contractName, abi, code, "", maxFeeLimit,
            0L, 100, 9223372036854775807L, "0", 0, null, testKeyForGrammarAddress3,
            grammarAddress3, blockingStubFull);
    Optional<TransactionInfo> infoById1 = null;
//    infoById1 = PublicMethed.getTransactionInfoById(contractAddress2, blockingStubFull);
//    Assert.assertTrue(infoById1.get().getResultValue() == 0);

    Assert.assertFalse(PublicMethed.updateEnergyLimit(contractAddress2, -1L,
        testKeyForGrammarAddress3, grammarAddress3, blockingStubFull));
    SmartContract smartContract = PublicMethed.getContract(contractAddress2, blockingStubFull);
    Assert.assertTrue(smartContract.getOriginEnergyLimit() == 9223372036854775807L);

    Assert.assertFalse(PublicMethed.updateEnergyLimit(contractAddress2, 0L,
        testKeyForGrammarAddress3, grammarAddress3, blockingStubFull));
    SmartContract smartContract1 = PublicMethed.getContract(contractAddress2, blockingStubFull);
    Assert.assertTrue(smartContract1.getOriginEnergyLimit() == 9223372036854775807L);

    Assert.assertTrue(PublicMethed.updateEnergyLimit(contractAddress2, 9223372036854775807L,
        testKeyForGrammarAddress3, grammarAddress3, blockingStubFull));
    SmartContract smartContract2 = PublicMethed.getContract(contractAddress2, blockingStubFull);
    Assert.assertTrue(smartContract2.getOriginEnergyLimit() == 9223372036854775807L);

    Assert.assertTrue(PublicMethed.updateEnergyLimit(contractAddress2, 'c',
        testKeyForGrammarAddress3, grammarAddress3, blockingStubFull));
    SmartContract smartContract3 = PublicMethed.getContract(contractAddress2, blockingStubFull);
    Assert.assertEquals(smartContract3.getOriginEnergyLimit(), 99);

    Assert.assertFalse(PublicMethed.updateEnergyLimit(contractAddress2, 1L,
        testNetAccountKey, testNetAccountAddress, blockingStubFull));
    SmartContract smartContract4 = PublicMethed.getContract(contractAddress2, blockingStubFull);
    Assert.assertEquals(smartContract4.getOriginEnergyLimit(), 99);
  }


}
