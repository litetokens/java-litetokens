package org.tron.core.db2.common;

import com.google.common.collect.Maps;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import lombok.Getter;
import org.rocksdb.WriteOptions;
import org.tron.common.storage.leveldb.RocksDbDataSourceImpl;
import org.tron.core.db.common.WrappedByteArray;

public class LevelDB implements DB<byte[], byte[]> {
  @Getter
  private RocksDbDataSourceImpl db;
  private WriteOptions writeOptions = new WriteOptions().setSync(true);

  public LevelDB(String parentName, String name) {
    db = new RocksDbDataSourceImpl(parentName, name);
    db.initDB();
  }

  @Override
  public byte[] get(byte[] key) {
    return db.getData(key);
  }

  @Override
  public void put(byte[] key, byte[] value) {
    db.putData(key, value);
  }

  @Override
  public void remove(byte[] key) {
    db.deleteData(key);
  }

  @Override
  public Iterator<Entry<byte[],byte[]>> iterator() {
    return db.iterator();
  }

  public void flush(Map<WrappedByteArray, WrappedByteArray> batch) {
    Map<byte[], byte[]> rows = batch.entrySet().stream()
        .map(e -> Maps.immutableEntry(e.getKey().getBytes(), e.getValue().getBytes()))
        .collect(HashMap::new, (m, k) -> m.put(k.getKey(), k.getValue()), HashMap::putAll);
    db.updateByBatch(rows, writeOptions);
  }

  public void close() {
    db.closeDB();
  }

  public void reset() {
    db.resetDb();
  }
}
