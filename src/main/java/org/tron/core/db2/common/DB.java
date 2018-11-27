package org.tron.core.db2.common;

import java.util.Map;

public interface DB<K, V> extends Iterable<Map.Entry<K, V>> {
  V get(K k);

  void put(K k, V v);

  void putAll(Map<Key, Value> map);

  Map<Key, Value> asMap();

  long size();

  boolean isEmpty();

  void remove(K k);
}
