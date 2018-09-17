package org.tron.core.exception;

public class BadBlockException extends TronException {

  public BadBlockException() {
    super();
  }

  public BadBlockException(String message) {
    super(message);
  }

  public BadBlockException(DupTransactionException e) {
    super(e.getMessage());
  }

}
