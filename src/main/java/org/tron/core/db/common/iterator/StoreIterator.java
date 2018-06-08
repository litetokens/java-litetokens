package org.tron.core.db.common.iterator;

import lombok.extern.slf4j.Slf4j;
import org.rocksdb.RocksIterator;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Map.Entry;
import java.util.function.Consumer;

@Slf4j
public final class StoreIterator implements org.tron.core.db.common.iterator.DBIterator {

  private RocksIterator dbIterator;
  private boolean first = true;

  public StoreIterator(RocksIterator dbIterator) {
    this.dbIterator = dbIterator;
  }

  @Override
  public void close() throws IOException {
    dbIterator.close();
  }

  @Override
  public boolean hasNext() {
    boolean hasNext = false;
    // true is first item
    try {
      if (first) {
        dbIterator.seekToFirst();
        first = false;
      }

      if (!(hasNext = dbIterator.isValid())) { // false is last item
        dbIterator.close();
      }
    } catch (Exception e) {
      logger.debug(e.getMessage(), e);
      dbIterator.close();
    }

    return hasNext;
  }

  @Override
  public Entry<byte[], byte[]> next() {
    return new AbstractMap.SimpleImmutableEntry<>(dbIterator.key(), dbIterator.value());
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void forEachRemaining(Consumer<? super Entry<byte[], byte[]>> action) {
    // TODO: how to implement ?
    // dbIterator.forEachRemaining(action);
  }
}
