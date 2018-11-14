package org.tron.common.runtime.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.tron.common.storage.Deposit;
import org.tron.core.Wallet;
import org.tron.core.actuator.TransferActuator;
import org.tron.core.actuator.TransferAssetActuator;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.config.args.Account;
import org.tron.core.exception.ContractValidateException;
import org.tron.protos.Protocol;

public class MUtil {
  private MUtil() {}

  public static void transfer(Deposit deposit, byte[] fromAddress, byte[] toAddress, long amount)
      throws ContractValidateException {
    if (0 == amount) {
      return;
    }
    TransferActuator.validateForSmartContract(deposit, fromAddress, toAddress, amount);
    deposit.addBalance(toAddress, amount);
    deposit.addBalance(fromAddress, -amount);
  }

  public static void transferAllToken(Deposit deposit, byte[] fromAddress, byte[] toAddress) {
    System.err.println("getAccount: " + System.nanoTime() / 1000);
    AccountCapsule fromAccountCap = deposit.getAccount(fromAddress);
    System.err.println("fromBuilder: " + System.nanoTime() / 1000);
    Protocol.Account.Builder fromBuilder = fromAccountCap.getInstance().toBuilder();
    System.err.println("toAccount: " + System.nanoTime() / 1000);
    AccountCapsule toAccountCap = deposit.getAccount(toAddress);
    System.err.println("toBuilder: " + System.nanoTime() / 1000);
    Protocol.Account.Builder toBuilder = toAccountCap.getInstance().toBuilder();
    System.err.println("beforLoopAssetMap: " + System.nanoTime() / 1000);
    fromAccountCap.getAssetMap().forEach((tokenId, amount) -> {
          toBuilder.putAsset(tokenId,toBuilder.getAssetMap().getOrDefault(tokenId, 0L) + amount);
          fromBuilder.putAsset(tokenId,0L);
        });
//    fromAccountCap.getAssetMap().forEach((tokenId, amount) ->
//        fromBuilder.putAsset(tokenId,0L));
    System.err.println("afterLoopAssetMap: " + System.nanoTime() / 1000);
    System.err.println("beforePutAccount1: " + System.nanoTime() / 1000);
    deposit.putAccountValue(fromAddress,new AccountCapsule(fromBuilder.build()));
    System.err.println("beforePutAccount2: " + System.nanoTime() / 1000);
    deposit.putAccountValue(toAddress, new AccountCapsule(toBuilder.build()));
    System.err.println("done: " + System.nanoTime() / 1000);
  }

  public static void transferToken(Deposit deposit, byte[] fromAddress, byte[] toAddress, String tokenId, long amount)
      throws ContractValidateException {
    if (0 == amount) {
      return;
    }
    TransferAssetActuator.validateForSmartContract(deposit, fromAddress, toAddress, tokenId.getBytes(), amount);
    deposit.addTokenBalance(toAddress, tokenId.getBytes(), amount);
    deposit.addTokenBalance(fromAddress, tokenId.getBytes(), -amount);
  }

  public static byte[] convertToTronAddress(byte[] address) {
    if (address.length == 20) {
      byte[] newAddress = new byte[21];
      byte[] temp = new byte[]{Wallet.getAddressPreFixByte()};
      System.arraycopy(temp, 0, newAddress, 0, temp.length);
      System.arraycopy(address, 0, newAddress, temp.length, address.length);
      address = newAddress;
    }
    return address;
  }
}
