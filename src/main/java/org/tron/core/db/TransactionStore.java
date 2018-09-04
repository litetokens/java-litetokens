package org.tron.core.db;

import com.google.common.collect.Streams;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.exception.BadItemException;
import org.tron.core.exception.StoreException;

@Slf4j
@Component
public class TransactionStore extends TronStoreWithRevoking<TransactionCapsule> {

  @Autowired
  private TransactionStore(@Value("trans") String dbName) {
    super(dbName);
  }

  @Override
  public void put(byte[] key, TransactionCapsule item) {
    super.put(key, item);
    if (Objects.nonNull(indexHelper)) {
      long start = System.currentTimeMillis();
      indexHelper.update(item.getInstance());
      logger.info("checkblock cost:" + (System.currentTimeMillis() - start));
    }
  }

  @Override
  public TransactionCapsule get(byte[] key) throws BadItemException {
    byte[] value = revokingDB.getUnchecked(key);
    return ArrayUtils.isEmpty(value) ? null : new TransactionCapsule(value);
  }

  /**
   * get total transaction.
   */
  public long getTotalTransactions() {
    return Streams.stream(iterator()).count();
  }

  @Override
  public void delete(byte[] key) {
    deleteIndex(key);
    super.delete(key);
  }

  private void deleteIndex(byte[] key) {
    if (Objects.nonNull(indexHelper)) {
      TransactionCapsule item;
      try {
        item = get(key);
        if (Objects.nonNull(item)) {
          indexHelper.remove(item.getInstance());
        }
      } catch (StoreException e) {
        return;
      }
    }
  }
}
