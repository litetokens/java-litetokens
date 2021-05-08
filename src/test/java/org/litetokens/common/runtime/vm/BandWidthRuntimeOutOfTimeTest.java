/*
 * java-litetokens is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * java-litetokens is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.litetokens.common.runtime.vm;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.io.File;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.litetokens.common.application.ApplicationFactory;
import org.litetokens.common.application.LitetokensApplicationContext;
import org.litetokens.common.runtime.Runtime;
import org.litetokens.common.runtime.RuntimeImpl;
import org.litetokens.common.runtime.LVMTestUtils;
import org.litetokens.common.runtime.vm.program.invoke.ProgramInvokeFactoryImpl;
import org.litetokens.common.storage.DepositImpl;
import org.litetokens.common.utils.FileUtil;
import org.litetokens.core.Constant;
import org.litetokens.core.Wallet;
import org.litetokens.core.capsule.AccountCapsule;
import org.litetokens.core.capsule.BlockCapsule;
import org.litetokens.core.capsule.TransactionCapsule;
import org.litetokens.core.config.DefaultConfig;
import org.litetokens.core.config.args.Args;
import org.litetokens.core.db.Manager;
import org.litetokens.core.db.TransactionTrace;
import org.litetokens.core.exception.AccountResourceInsufficientException;
import org.litetokens.core.exception.ContractExeException;
import org.litetokens.core.exception.ContractValidateException;
import org.litetokens.core.exception.TooBigTransactionResultException;
import org.litetokens.core.exception.LitetokensException;
import org.litetokens.core.exception.VMIllegalException;
import org.litetokens.protos.Contract.CreateSmartContract;
import org.litetokens.protos.Contract.TriggerSmartContract;
import org.litetokens.protos.Protocol.AccountType;
import org.litetokens.protos.Protocol.Transaction;
import org.litetokens.protos.Protocol.Transaction.Contract;
import org.litetokens.protos.Protocol.Transaction.Contract.ContractType;
import org.litetokens.protos.Protocol.Transaction.raw;

/**
 * pragma solidity ^0.4.2;
 *
 * contract Fibonacci {
 *
 * event Notify(uint input, uint result);
 *
 * function fibonacci(uint number) constant returns(uint result) { if (number == 0) { return 0; }
 * else if (number == 1) { return 1; } else { uint256 first = 0; uint256 second = 1; uint256 ret =
 * 0; for(uint256 i = 2; i <= number; i++) { ret = first + second; first = second; second = ret; }
 * return ret; } }
 *
 * function fibonacciNotify(uint number) returns(uint result) { result = fibonacci(number);
 * Notify(number, result); } }
 */
public class BandWidthRuntimeOutOfTimeTest {

  public static final long totalBalance = 1000_0000_000_000L;
  private static String dbPath = "output_BandWidthRuntimeOutOfTimeTest_test";
  private static String dbDirectory = "db_BandWidthRuntimeOutOfTimeTest_test";
  private static String indexDirectory = "index_BandWidthRuntimeOutOfTimeTest_test";
  private static AnnotationConfigApplicationContext context;
  private static Manager dbManager;

  private static String OwnerAddress = "LSW3JYeKerzNUrjvWSu5s7yhsb1ZQWFAf2";
  private String xlt2ContractAddress = "LcjepiM3vhk27Z7uREq3GGu7tBq6qH4z19";
  private static String TriggerOwnerAddress = "LeDgf3bhFNrpMpPd3XEvkVo1qBY2y7e1Lz";

  static {
    Args.setParam(
        new String[]{
            "--output-directory", dbPath,
            "--storage-db-directory", dbDirectory,
            "--storage-index-directory", indexDirectory,
            "-w",
            "--debug"
        },
        "config-test-mainnet.conf"
    );
    context = new LitetokensApplicationContext(DefaultConfig.class);
  }

  /**
   * Init data.
   */
  @BeforeClass
  public static void init() {
    dbManager = context.getBean(Manager.class);
    //init energy
    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(1526647828000L);
    dbManager.getDynamicPropertiesStore().saveTotalEnergyWeight(10_000_000L);

    dbManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(0);

    AccountCapsule accountCapsule = new AccountCapsule(ByteString.copyFrom("owner".getBytes()),
        ByteString.copyFrom(Wallet.decodeFromBase58Check(OwnerAddress)), AccountType.Normal,
        totalBalance);

    accountCapsule.setFrozenForEnergy(10_000_000L, 0L);
    dbManager.getAccountStore()
        .put(Wallet.decodeFromBase58Check(OwnerAddress), accountCapsule);

    AccountCapsule accountCapsule2 = new AccountCapsule(ByteString.copyFrom("owner".getBytes()),
        ByteString.copyFrom(Wallet.decodeFromBase58Check(TriggerOwnerAddress)), AccountType.Normal,
        totalBalance);

    accountCapsule2.setFrozenForEnergy(10_000_000L, 0L);
    dbManager.getAccountStore()
        .put(Wallet.decodeFromBase58Check(TriggerOwnerAddress), accountCapsule2);
    dbManager.getDynamicPropertiesStore()
        .saveLatestBlockHeaderTimestamp(System.currentTimeMillis() / 1000);
  }

  @Test
  public void testSuccess() {
    try {
      byte[] contractAddress = createContract();
      AccountCapsule triggerOwner = dbManager.getAccountStore()
          .get(Wallet.decodeFromBase58Check(TriggerOwnerAddress));
      long energy = triggerOwner.getEnergyUsage();
      long balance = triggerOwner.getBalance();
      TriggerSmartContract triggerContract = LVMTestUtils.createTriggerContract(contractAddress,
          "fibonacciNotify(uint256)", "500000", false,
          0, Wallet.decodeFromBase58Check(TriggerOwnerAddress));
      Transaction transaction = Transaction.newBuilder().setRawData(raw.newBuilder().addContract(
          Contract.newBuilder().setParameter(Any.pack(triggerContract))
              .setType(ContractType.TriggerSmartContract)).setFeeLimit(100000000000L)).build();
      TransactionCapsule xltCap = new TransactionCapsule(transaction);
      TransactionTrace trace = new TransactionTrace(xltCap, dbManager);
      dbManager.consumeBandwidth(xltCap, trace);
      BlockCapsule blockCapsule = null;
      DepositImpl deposit = DepositImpl.createRoot(dbManager);
      Runtime runtime = new RuntimeImpl(trace, blockCapsule, deposit,
          new ProgramInvokeFactoryImpl());
      trace.init(blockCapsule);
      trace.exec();
      trace.finalization();

      triggerOwner = dbManager.getAccountStore()
          .get(Wallet.decodeFromBase58Check(TriggerOwnerAddress));
      energy = triggerOwner.getEnergyUsage() - energy;
      balance = balance - triggerOwner.getBalance();
      Assert.assertNotNull(runtime.getRuntimeError());
      Assert.assertTrue(runtime.getRuntimeError().contains(" timeout "));
      Assert.assertEquals(9950000, trace.getReceipt().getEnergyUsageTotal());
      Assert.assertEquals(50000, energy);
      Assert.assertEquals(990000000, balance);
      Assert.assertEquals(9950000 * Constant.SUN_PER_ENERGY,
          balance + energy * Constant.SUN_PER_ENERGY);
    } catch (LitetokensException e) {
      Assert.assertNotNull(e);
    }
  }

  private byte[] createContract()
      throws ContractValidateException, AccountResourceInsufficientException, TooBigTransactionResultException, ContractExeException, VMIllegalException {
    AccountCapsule owner = dbManager.getAccountStore()
        .get(Wallet.decodeFromBase58Check(OwnerAddress));
    long energy = owner.getEnergyUsage();
    long balance = owner.getBalance();

    String contractName = "Fibonacci3";
    String code = "608060405234801561001057600080fd5b506101ba806100206000396000f30060806040526004361061004c576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff1680633c7fdc701461005157806361047ff414610092575b600080fd5b34801561005d57600080fd5b5061007c600480360381019080803590602001909291905050506100d3565b6040518082815260200191505060405180910390f35b34801561009e57600080fd5b506100bd60048036038101908080359060200190929190505050610124565b6040518082815260200191505060405180910390f35b60006100de82610124565b90507f71e71a8458267085d5ab16980fd5f114d2d37f232479c245d523ce8d23ca40ed8282604051808381526020018281526020019250505060405180910390a1919050565b60008060008060008086141561013d5760009450610185565b600186141561014f5760019450610185565b600093506001925060009150600290505b85811115156101815782840191508293508192508080600101915050610160565b8194505b505050509190505600a165627a7a7230582071f3cf655137ce9dc32d3307fb879e65f3960769282e6e452a5f0023ea046ed20029";
    String abi = "[{\"constant\":false,\"inputs\":[{\"name\":\"number\",\"type\":\"uint256\"}],\"name\":\"fibonacciNotify\",\"outputs\":[{\"name\":\"result\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"constant\":true,\"inputs\":[{\"name\":\"number\",\"type\":\"uint256\"}],\"name\":\"fibonacci\",\"outputs\":[{\"name\":\"result\",\"type\":\"uint256\"}],\"payable\":false,\"stateMutability\":\"view\",\"type\":\"function\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":false,\"name\":\"input\",\"type\":\"uint256\"},{\"indexed\":false,\"name\":\"result\",\"type\":\"uint256\"}],\"name\":\"Notify\",\"type\":\"event\"}]";
    CreateSmartContract smartContract = LVMTestUtils.createSmartContract(
        Wallet.decodeFromBase58Check(OwnerAddress), contractName, abi, code, 0, 100);
    Transaction transaction = Transaction.newBuilder().setRawData(raw.newBuilder().addContract(
        Contract.newBuilder().setParameter(Any.pack(smartContract))
            .setType(ContractType.CreateSmartContract)).setFeeLimit(1000000000)).build();
    TransactionCapsule xltCap = new TransactionCapsule(transaction);
    TransactionTrace trace = new TransactionTrace(xltCap, dbManager);
    dbManager.consumeBandwidth(xltCap, trace);
    BlockCapsule blockCapsule = null;
    DepositImpl deposit = DepositImpl.createRoot(dbManager);
    Runtime runtime = new RuntimeImpl(trace, blockCapsule, deposit, new ProgramInvokeFactoryImpl());
    trace.init(blockCapsule);
    trace.exec();
    trace.finalization();
    owner = dbManager.getAccountStore()
        .get(Wallet.decodeFromBase58Check(OwnerAddress));
    energy = owner.getEnergyUsage() - energy;
    balance = balance - owner.getBalance();
    Assert.assertEquals(88529, trace.getReceipt().getEnergyUsageTotal());
    Assert.assertEquals(50000, energy);
    Assert.assertEquals(3852900, balance);
    Assert.assertEquals(88529 * 100, balance + energy * 100);
    if (runtime.getRuntimeError() != null) {
      return runtime.getResult().getContractAddress();
    }
    return runtime.getResult().getContractAddress();

  }

  /**
   * destroy clear data of testing.
   */
  @AfterClass
  public static void destroy() {
    Args.clearParam();
    ApplicationFactory.create(context).shutdown();
    context.destroy();
    FileUtil.deleteDir(new File(dbPath));
  }
}