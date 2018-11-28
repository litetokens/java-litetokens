package org.tron.core.db2.core;

import com.google.common.collect.Maps;
import com.google.common.collect.Streams;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
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

    ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
    service.scheduleWithFixedDelay(() -> {
      try {
        double ratio = missMap.getOrDefault(dbName, 0L) * 1.0 / hitMap.getOrDefault(dbName, 1L) * 100;
        logger.error("db:" + dbName + "miss-rate:" + ratio + "% hit:" + hitMap.get(dbName) + " miss:"+missMap.get(dbName) + " access:" + accessMap.get(dbName));
      } catch (Throwable t) {
        logger.error("Exception in log worker", t);
      }
    }, 10, 30, TimeUnit.SECONDS);
  }

  Map<String, Long> hitMap = new ConcurrentHashMap<>();
  Map<String, Long> accessMap = new ConcurrentHashMap<>();
  Map<String, Long> missMap = new ConcurrentHashMap<>();

  @Override
  public byte[] get(byte[] key) {
    byte[] val = db.get(key);
    if (!ArrayUtils.isEmpty(key)) {
      long valLen = ArrayUtils.isEmpty(val) ? 0 : val.length;
      if (!hitMap.containsKey(dbName)) {
        hitMap.put(dbName, 1L);
        accessMap.put(dbName, 0L);
        if (valLen == 0) {
          missMap.put(dbName, valLen);
        } else {
          missMap.put(dbName, 0L);
        }
      } else {
        hitMap.put(dbName, hitMap.get(dbName) + 1);
        accessMap.put(dbName, accessMap.get(dbName) + valLen);
        if (valLen == 0) {
          missMap.put(dbName, missMap.get(dbName) + 1);
        }
      }
//      logger.error("GET:" + dbName + " key:" + key.length + " val:" + (val == null ? "0 " : val.length));
    }
    return val;
  }

  @Override
  public void put(byte[] key, byte[] value) {
//    if (ArrayUtils.isNotEmpty(key)) {
//      logger.error("PUT:" + dbName + " key:" + key.length + " val:" + (ArrayUtils.isEmpty(value) ? "0 " : value.length));
//    }
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
