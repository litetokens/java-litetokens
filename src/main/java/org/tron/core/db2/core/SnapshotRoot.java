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
import lombok.extern.slf4j.Slf4j;
import org.spongycastle.util.encoders.Hex;
import org.tron.core.db.common.WrappedByteArray;
import org.tron.core.db2.common.LevelDB;

@Slf4j(topic = "io")
public class SnapshotRoot extends AbstractSnapshot<byte[], byte[]> {

  @Getter
  private Snapshot solidity;

  private String dbName;

  public SnapshotRoot(String parentName, String name) {
    db = new LevelDB(parentName, name);
    dbName = name;
    solidity = this;
  }

  @Override
  public byte[] get(byte[] key) {
    byte[] val = db.get(key);
    logger.error("db:" + dbName + " key:" + key.length + " val:" + val.length);
    return val;
  }

  @Override
  public void put(byte[] key, byte[] value) {
    db.put(key, value);
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
  public Iterator<Map.Entry<byte[],byte[]>> iterator() {
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
