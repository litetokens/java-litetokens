package org.tron.core.exception;

import org.tron.core.capsule.TransactionCapsule;

public class DupTransactionException extends TronException {

  public DupTransactionException() {
    super();
  }

  public DupTransactionException(String message) {
    super(message);
  }

  public DupTransactionException(TransactionCapsule tx) {
    super("Find dup Transaction: " + tx.toString());
  }
}
