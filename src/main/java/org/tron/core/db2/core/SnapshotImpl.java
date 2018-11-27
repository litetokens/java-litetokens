package org.tron.core.db2.core;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.collect.Streams;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import org.tron.core.db.common.WrappedByteArray;
import org.tron.core.db2.common.HashDB;
import org.tron.core.db2.common.Key;
import org.tron.core.db2.common.Value;

public class SnapshotImpl extends AbstractSnapshot<Key, Value> {
  @Getter
  protected Snapshot root;

  SnapshotImpl(Snapshot snapshot) {
    root = snapshot.getRoot();
    previous = snapshot;
    snapshot.setNext(this);
    db = new HashDB();
    if (Snapshot.isImpl(snapshot)) {
      db.putAll(((SnapshotImpl) snapshot).db.asMap());
    }
  }

  @Override
  public byte[] get(byte[] key) {
    return get(this, key);
  }

  @Override
  public void put(byte[] key, byte[] value) {
    Preconditions.checkNotNull(key, "key in db is not null.");
    Preconditions.checkNotNull(value, "value in db is not null.");

    Value.Operator operator;
    byte[] v;
    if ((v = get(previous, key)) == null) {
      operator = Value.Operator.CREATE;
    } else {
      operator = Value.Operator.MODIFY;
    }
    db.put(Key.copyOf(key), Value.copyOf(operator, value));
  }

  @Override
  public void remove(byte[] key) {
    Preconditions.checkNotNull(key, "key in db is not null.");

    if (get(key) != null) {
      db.put(Key.of(key), Value.of(Value.Operator.DELETE, null));
    }
  }

  private byte[] get(Snapshot head, byte[] key) {
    if (head == null) {
      return null;
    }

    byte[] v;
    if ((v = head.get(key)) == null) {
      v = head.getRoot().get(key);
    }

    return v;
  }

  @Override
  public void merge(Snapshot from) {
    SnapshotImpl fromImpl = (SnapshotImpl) from;

    db.putAll(fromImpl.db.asMap());
  }

  @Override
  public Snapshot retreat() {
    return previous;
  }

  @Override
  public Snapshot getSolidity() {
    return root.getSolidity();
  }

  @Override
  public Iterator<Map.Entry<byte[],byte[]>> iterator() {
    Map<WrappedByteArray, WrappedByteArray> all = new HashMap<>();
    collect(all);
    Set<WrappedByteArray> keys = new HashSet<>(all.keySet());
    all.entrySet()
        .removeIf(entry -> entry.getValue() == null || entry.getValue().getBytes() == null);
    return Iterators.concat(
        Iterators.transform(all.entrySet().iterator(),
            e -> Maps.immutableEntry(e.getKey().getBytes(), e.getValue().getBytes())),
        Iterators.filter(getRoot().iterator(),
            e -> !keys.contains(WrappedByteArray.of(e.getKey()))));
  }

  void collect(Map<WrappedByteArray, WrappedByteArray> all) {
    Snapshot next = getRoot().getNext();
    while (next != null) {
      Streams.stream(((SnapshotImpl) next).db)
          .forEach(e -> all.put(WrappedByteArray.of(e.getKey().getBytes()),
              WrappedByteArray.of(e.getValue().getBytes())));
      next = next.getNext();
    }
  }

  @Override
  public void close() {
    getRoot().close();
  }

  @Override
  public void reset() {
    getRoot().reset();
  }

  @Override
  public void resetSolidity() {
    root.resetSolidity();
  }

  @Override
  public void updateSolidity() {
    root.updateSolidity();
  }
}
