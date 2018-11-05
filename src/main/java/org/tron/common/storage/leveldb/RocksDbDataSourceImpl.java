package org.tron.common.storage.leveldb;

import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.rocksdb.WriteOptions;
import org.tron.common.storage.DbSourceInter;
import org.tron.common.utils.FileUtil;
import org.tron.core.config.args.Args;
import org.tron.core.db.common.iterator.RockStoreIterator;
import org.tron.core.db.common.iterator.StoreIterator;
import org.tron.core.exception.BadItemException;

/**
 * @program: java-tron
 * @description:
 * @author: shydesky@gmail.com
 * @create: 2018-10-30
 **/

@Slf4j
@NoArgsConstructor
public class RocksDbDataSourceImpl implements DbSourceInter<byte[]>,
    Iterable<Map.Entry<byte[], byte[]>> {

  private String dataBaseName;
  private RocksDB database;
  private boolean alive;
  private String parentName;
  @Getter
  private String newParentName;
  private ReadWriteLock resetDbLock = new ReentrantReadWriteLock();

  public RocksDbDataSourceImpl(String parentName, String name) {
    this.dataBaseName = name;
    this.parentName = Paths.get(
        parentName,
        Args.getInstance().getStorage().getDbDirectory()
    ).toString();

    this.newParentName = Paths.get(
        parentName,
        Args.getInstance().getStorage().getDbDirectory()
    ).toString().replace("output-rocksdb2", "output-rocksdb3");
  }

  public Path getDbPath() {
    return Paths.get(parentName, dataBaseName);
  }

  public Path getBackupDbPath() {
    return Paths.get(newParentName, dataBaseName);
  }

  public RocksDB getDatabase() {
    return database;
  }

  public boolean isAlive() {
    return alive;
  }

  @Override
  public void closeDB() {
    resetDbLock.writeLock().lock();
    try {
      if (!isAlive()) {
        return;
      }
      database.close();
      alive = false;
    } finally {
      resetDbLock.writeLock().unlock();
    }
  }

  @Override
  public void resetDb() {
    closeDB();
    FileUtil.recursiveDelete(getDbPath().toString());
    initDB();
  }

  @Override
  public Set<byte[]> allKeys() throws RuntimeException {
    resetDbLock.readLock().lock();
    Set<byte[]> result = Sets.newHashSet();

    try (final RocksIterator iter = database.newIterator()) {
      for (iter.seekToFirst(); iter.isValid(); iter.next()) {
        result.add(iter.key());
      }
      return result;
    } finally {
      resetDbLock.readLock().unlock();
    }
  }

  @Override
  public Set<byte[]> allValues() throws RuntimeException {
    return null;
  }

  @Override
  public long getTotal() throws RuntimeException {
    return 0;
  }

  @Override
  public String getDBName() {
    return this.dataBaseName;
  }

  @Override
  public void setDBName(String name) {

  }

  public void initDB() {
    resetDbLock.writeLock().lock();
    try {
      if (isAlive()) {
        return;
      }

      if (dataBaseName == null) {
        throw new NullPointerException("no name set to the dbStore");
      }

      this.database = openDatabse();
      alive = true;
    } catch (IOException e) {
      throw new RuntimeException("Can't initialize database", e);
    } finally {
      resetDbLock.writeLock().unlock();
    }
    return;
  }

  private RocksDB openDatabse() throws IOException {
    try {
      final Options options = new Options().setCreateIfMissing(true);
      Path dbPath = getDbPath();
      if (!Files.isSymbolicLink(dbPath.getParent())) {
        Files.createDirectories(dbPath.getParent());
      }
      logger.info("dbPath:{}", dbPath.toString());
      return RocksDB.open(options, dbPath.toString());
    } catch (RocksDBException e) {
      logger.error("RocksDBException:{}", e);
    }
    return null;
  }

  @Override
  public void putData(byte[] key, byte[] value) {
    resetDbLock.readLock().lock();
    try {
      database.put(key, value);
    } catch (RocksDBException e) {
      logger.error("RocksDBException:{}", e);
    } finally {
      resetDbLock.readLock().unlock();
    }
  }

  @Override
  public void putData(byte[] key, byte[] value, WriteOptions writeOpt) {
    resetDbLock.readLock().lock();
    try {
      database.put(writeOpt, key, value);
    } catch (RocksDBException e) {
      logger.error("RocksDBException:{}", e);
    } finally {
      resetDbLock.readLock().unlock();
    }
  }

  @Override
  public byte[] getData(byte[] key) {
    resetDbLock.readLock().lock();
    try {
      return database.get(key);
    } catch (RocksDBException e) {
      logger.error("RocksDBException: {}", e);
    } finally {
      resetDbLock.readLock().unlock();
    }
    return null;
  }

  @Override
  public void deleteData(byte[] key) {
    resetDbLock.readLock().lock();
    try {
      database.delete(key);
    } catch (RocksDBException e) {
      logger.error("RocksDBException:{}", e);
    } finally {
      resetDbLock.readLock().unlock();
    }
  }

  @Override
  public void deleteData(byte[] key, WriteOptions writeOpt) {
    resetDbLock.readLock().lock();
    try {
      database.delete(writeOpt, key);
    } catch (RocksDBException e) {
      logger.error("RocksDBException:{}", e);
    } finally {
      resetDbLock.readLock().unlock();
    }
  }

  @Override
  public boolean flush() {
    return false;
  }

  @Override
  public org.tron.core.db.common.iterator.DBIterator iterator() {
    return new RockStoreIterator(database.newIterator());
  }

  @Override
  public void updateByBatch(Map<byte[], byte[]> rows) {

  }

  @Override
  public void updateByBatch(Map<byte[], byte[]> rows, WriteOptions writeOptions) {

  }

  public Map<byte[], byte[]> getNext(byte[] key, long limit) {
    if (limit <= 0) {
      return Collections.emptyMap();
    }
    resetDbLock.readLock().lock();
    try (RocksIterator iter = database.newIterator()) {
      Map<byte[], byte[]> result = new HashMap<>();
      long i = 0;
      for (iter.seek(key); iter.isValid() && i < limit; iter.next(), i++) {
        result.put(iter.key(), iter.value());
      }
      return result;
    } finally {
      resetDbLock.readLock().unlock();
    }
  }

  public Set<byte[]> getlatestValues(long limit) {
    if (limit <= 0) {
      return Sets.newHashSet();
    }
    resetDbLock.readLock().lock();

    try (RocksIterator iter = database.newIterator()) {
      Set<byte[]> result = Sets.newHashSet();
      long i = 0;
      for (iter.seekToLast(); iter.isValid() && i < limit; iter.prev(), i++) {
        result.add(iter.value());
      }
      return result;
    } finally {
      resetDbLock.readLock().unlock();
    }
  }

  public Set<byte[]> getValuesPrev(byte[] key, long limit) {
    if (limit <= 0) {
      return Sets.newHashSet();
    }
    resetDbLock.readLock().lock();
    try (RocksIterator iter = database.newIterator()) {
      Set<byte[]> result = Sets.newHashSet();
      long i = 0;
      byte[] data = getData(key);
      if (Objects.nonNull(data)) {
        result.add(data);
        i++;
      }

      for (iter.seekForPrev(key); iter.isValid() && i < limit; iter.prev(), i++) {
        result.add(iter.value());
      }
      return result;
    } finally {
      resetDbLock.readLock().unlock();
    }
  }

  public Set<byte[]> getValuesNext(byte[] key, long limit) {
    if (limit <= 0) {
      return Sets.newHashSet();
    }
    resetDbLock.readLock().lock();
    try (RocksIterator iter = database.newIterator()) {
      Set<byte[]> result = Sets.newHashSet();
      long i = 0;
      for (iter.seek(key); iter.isValid() && i < limit; iter.next(), i++) {
        result.add(iter.value());
      }
      return result;
    } finally {
      resetDbLock.readLock().unlock();
    }
  }
}