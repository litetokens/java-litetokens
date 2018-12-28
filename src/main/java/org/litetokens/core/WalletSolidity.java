package org.litetokens.core;

import com.google.protobuf.ByteString;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.litetokens.api.GrpcAPI.TransactionList;
import org.litetokens.common.utils.ByteArray;
import org.litetokens.core.capsule.TransactionInfoCapsule;
import org.litetokens.core.db.Manager;
import org.litetokens.core.db.api.StoreAPI;
import org.litetokens.core.exception.BadItemException;
import org.litetokens.core.exception.NonUniqueObjectException;
import org.litetokens.core.exception.StoreException;
import org.litetokens.protos.Protocol.Transaction;
import org.litetokens.protos.Protocol.TransactionInfo;

@Slf4j
@Component
public class WalletSolidity {

  @Autowired
  private StoreAPI storeAPI;

  public TransactionList getTransactionsFromThis(ByteString thisAddress, long offset, long limit) {
    List<Transaction> transactionsFromThis = storeAPI
        .getTransactionsFromThis(ByteArray.toHexString(thisAddress.toByteArray()), offset, limit);
    TransactionList transactionList = TransactionList.newBuilder()
        .addAllTransaction(transactionsFromThis).build();
    return transactionList;
  }

  public TransactionList getTransactionsToThis(ByteString toAddress, long offset, long limit) {
    List<Transaction> transactionsToThis = storeAPI
        .getTransactionsToThis(ByteArray.toHexString(toAddress.toByteArray()), offset, limit);
    TransactionList transactionList = TransactionList.newBuilder()
        .addAllTransaction(transactionsToThis).build();
    return transactionList;
  }
}
