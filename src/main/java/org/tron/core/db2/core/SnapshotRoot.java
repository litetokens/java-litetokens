package org.tron.core.db2.core;

import com.google.common.collect.Maps;
import com.google.common.collect.Streams;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;
import org.apache.commons.lang3.ArrayUtils;
import org.tron.common.runtime.utils.PerformanceHelper;
import org.tron.core.db.common.WrappedByteArray;
import org.tron.core.db2.common.LevelDB;

public class SnapshotRoot extends AbstractSnapshot<byte[], byte[]> {

  @Getter
  private Snapshot solidity;

  private String dbName;

  public SnapshotRoot(String parentName, String name) {
    db = new LevelDB(parentName, name);
    this.dbName = name;
    solidity = this;
  }

  @Override
  public byte[] get(byte[] key) {

    long preMs = System.nanoTime() / 1000;
    byte[] val = db.get(key);
    long consume = System.nanoTime() / 1000 - preMs;
    long keyLength = 0;
    long valLength = 0;
    if (key != null) {
      keyLength = key.length;
    }
    if (val != null) {
      valLength = val.length;
    }
    PerformanceHelper.singleTxGetPutInfo.add(
        this.dbName + "\1GET\1" + String.valueOf(keyLength) + "\1" + String.valueOf(valLength)
            + "\1"
            + String.valueOf(consume));
    return val;
  }

  @Override
  public void put(byte[] key, byte[] value) {
    long preMs = System.nanoTime() / 1000;
    db.put(key, value);
    long consume = System.nanoTime() / 1000 - preMs;
    long keyLength = 0;
    long valLength = 0;
    if (key != null) {
      keyLength = key.length;
    }
    if (value != null) {
      valLength = value.length;
    }
    PerformanceHelper.singleTxGetPutInfo.add(
        this.dbName + "\1PUT\1" + String.valueOf(keyLength) + "\1" + String.valueOf(valLength)
            + "\1" + String.valueOf(consume));
  }

  @Override
  public void remove(byte[] key) {
    db.remove(key);
  }

  @Override
  public void merge(Snapshot from) {
    LevelDB levelDB = (LevelDB) db;
    SnapshotImpl snapshot = (SnapshotImpl) from;
    Map<WrappedByteArray, WrappedByteArray> batch = Streams.stream(snapshot.db)
        .map(e -> Maps.immutableEntry(WrappedByteArray.of(e.getKey().getBytes()),
            WrappedByteArray.of(e.getValue().getBytes())))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    levelDB.flush(batch);
  }

  public void merge(List<Snapshot> snapshots) {
    Map<WrappedByteArray, WrappedByteArray> batch = new HashMap<>();
    for (Snapshot snapshot : snapshots) {
      SnapshotImpl from = (SnapshotImpl) snapshot;
      Streams.stream(from.db)
          .map(e -> Maps.immutableEntry(WrappedByteArray.of(e.getKey().getBytes()),
              WrappedByteArray.of(e.getValue().getBytes())))
          .forEach(e -> batch.put(e.getKey(), e.getValue()));
    }
    ((LevelDB) db).flush(batch);
  }

  @Override
  public Snapshot retreat() {
    return this;
  }

  @Override
  public Snapshot getRoot() {
    return this;
  }

  @Override
  public Iterator<Map.Entry<byte[], byte[]>> iterator() {
    return db.iterator();
  }

  @Override
  public void close() {
    ((LevelDB) db).close();
  }

  @Override
  public void reset() {
    ((LevelDB) db).reset();
  }

  @Override
  public void resetSolidity() {
    solidity = this;
  }

  @Override
  public void updateSolidity() {
    solidity = solidity.getNext();
  }
}
