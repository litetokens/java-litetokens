/*
 * Copyright (c) [2016] [ <ether.camp> ] This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with the ethereumJ
 * library. If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.common.storage.leveldb;

import com.google.common.collect.Sets;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.rocksdb.*;

import org.rocksdb.RocksDBException;
import org.tron.common.storage.DbSourceInter;
import org.tron.common.utils.FileUtil;
import org.tron.core.config.args.Args;
import org.tron.core.db.common.iterator.StoreIterator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Slf4j
@NoArgsConstructor
public class LevelDbDataSourceImpl implements DbSourceInter<byte[]>,
    Iterable<Entry<byte[], byte[]>> {

  static {
    // a static method that loads the RocksDB C++ library.
    RocksDB.loadLibrary();
  }

  private String dataBaseName;
  private RocksDB database;
  private boolean alive;
  private String parentName;
  private ReadWriteLock resetDbLock = new ReentrantReadWriteLock();

  /**
   * constructor.
   */
  public LevelDbDataSourceImpl(String parentName, String name) {
    this.dataBaseName = name;
    this.parentName = Paths.get(
        parentName,
        Args.getInstance().getStorage().getDbDirectory()
    ).toString();
  }

  @Override
  public void initDB() {
    resetDbLock.writeLock().lock();
    try {
      logger.debug("~> LevelDbDataSourceImpl.initDB(): " + dataBaseName);

      if (isAlive()) {
        return;
      }

      if (dataBaseName == null) {
        throw new NullPointerException("no name set to the dbStore");
      }

      // TODO: get from configs
      Options dbOptions = new Options().setCreateIfMissing(true);

      try {
        openDatabase(dbOptions);
        alive = true;
      } catch (IOException | RocksDBException e) {
        throw new RuntimeException("Can't initialize database", e);
      }
    } finally {
      resetDbLock.writeLock().unlock();
    }
  }

  private void openDatabase(Options dbOptions) throws IOException, RocksDBException {
    final Path dbPath = getDbPath();
    if (!Files.isSymbolicLink(dbPath.getParent())) {
      Files.createDirectories(dbPath.getParent());
    }
    try {
      database = RocksDB.open(dbOptions, dbPath.toString());
    } catch (RocksDBException e) {
      // TODO: what to do, repair ?
      throw e;
    }
  }

  private Path getDbPath() {
    return Paths.get(parentName, dataBaseName);
  }

  /**
   * reset database.
   */
  public void resetDb() {
    closeDB();
    FileUtil.recursiveDelete(getDbPath().toString());
    initDB();
  }

  @Override
  public boolean isAlive() {
    return alive;
  }

  /**
   * destroy database.
   */
  public void destroyDb(File fileLocation) {
    resetDbLock.writeLock().lock();
    try {
      logger.debug("Destroying existing database: " + fileLocation);
      Options options = new Options();
      try {
        RocksDB.destroyDB(fileLocation.toString(), options);
      } catch (RocksDBException e) {
        logger.error(e.getMessage(), e);
      }
    } finally {
      resetDbLock.writeLock().unlock();
    }
  }

  @Override
  public String getDBName() {
    return dataBaseName;
  }

  @Override
  public void setDBName(String name) {
    this.dataBaseName = name;
  }

  @Override
  public byte[] getData(byte[] key) {
    resetDbLock.readLock().lock();
    try {
      return database.get(key);
    } catch (RocksDBException e) {
      logger.debug(e.getMessage(), e);
    } finally {
      resetDbLock.readLock().unlock();
    }
    return null;
  }

  @Override
  public void putData(byte[] key, byte[] value) {
    resetDbLock.readLock().lock();
    try {
      database.put(key, value);
    } catch (RocksDBException e) {
      logger.debug(e.getMessage(), e);
    } finally {
      resetDbLock.readLock().unlock();
    }
  }

  @Override
  public void putData(byte[] key, byte[] value, WriteOptions options) {
    resetDbLock.readLock().lock();
    try {
      database.put(options, key, value);
    } catch (RocksDBException e) {
      logger.debug(e.getMessage(), e);
    } finally {
      resetDbLock.readLock().unlock();
    }
  }

  @Override
  public void deleteData(byte[] key) {
    resetDbLock.readLock().lock();
    try {
      database.delete(key);
    } catch (RocksDBException e) {
      logger.debug(e.getMessage(), e);
    } finally {
      resetDbLock.readLock().unlock();
    }
  }

  @Override
  public void deleteData(byte[] key, WriteOptions options) {
    resetDbLock.readLock().lock();
    try {
      database.delete(options, key);
    } catch (RocksDBException e) {
      logger.debug(e.getMessage(), e);
    } finally {
      resetDbLock.readLock().unlock();
    }
  }

  @Deprecated
  @Override
  public Set<byte[]> allKeys() {
    resetDbLock.readLock().lock();
    try (RocksIterator iterator = database.newIterator()) {
      Set<byte[]> result = Sets.newHashSet();
      for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
        result.add(iterator.key());
      }
      return result;
    } finally {
      resetDbLock.readLock().unlock();
    }
  }

  @Deprecated
  @Override
  public Set<byte[]> allValues() {
    resetDbLock.readLock().lock();
    try (RocksIterator iterator = database.newIterator()) {
      Set<byte[]> result = Sets.newHashSet();
      for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
        result.add(iterator.value());
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
    try (RocksIterator iterator = database.newIterator()) {
      Set<byte[]> result = Sets.newHashSet();
      long i = 0;
      iterator.seekToLast();
      if (iterator.isValid()) {
        result.add(iterator.value());
        i++;
      }
      for (; iterator.isValid() && i++ < limit; iterator.prev()) {
        result.add(iterator.value());
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
    try (RocksIterator iterator = database.newIterator()) {
      Set<byte[]> result = Sets.newHashSet();
      long i = 0;
      for (iterator.seek(key); iterator.isValid() && i++ < limit; iterator.next()) {
        result.add(iterator.value());
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
    try (RocksIterator iterator = database.newIterator()) {
      Set<byte[]> result = Sets.newHashSet();
      long i = 0;
      byte[] data = getData(key);
      if (Objects.nonNull(data)) {
        result.add(data);
        i++;
      }
      iterator.seek(key);
      if (iterator.isValid()) {
        iterator.prev();
      }
      for (; iterator.isValid() && i++ < limit; iterator.prev()) {
        result.add(iterator.value());
      }
      return result;
    } finally {
      resetDbLock.readLock().unlock();
    }
  }

  @Override
  public long getTotal() throws RuntimeException {
    resetDbLock.readLock().lock();
    try (RocksIterator iterator = database.newIterator()) {
      long total = 0;
      for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
        total++;
      }
      return total;
    } finally {
      resetDbLock.readLock().unlock();
    }
  }

  private void updateByBatchInner(Map<byte[], byte[]> rows) throws Exception {
    try (WriteBatch batch = new WriteBatch()) {
      rows.forEach((key, value) -> {
        try {
          if (value == null) {
            batch.delete(key);
          } else {
            batch.put(key, value);
          }
        } catch (RocksDBException e) {
          e.printStackTrace();
        }
      });
      database.write(new WriteOptions(), batch);
    }
  }

  @Override
  public void updateByBatch(Map<byte[], byte[]> rows) {
    resetDbLock.readLock().lock();
    try {
      updateByBatchInner(rows);
    } catch (Exception e) {
      try {
        updateByBatchInner(rows);
      } catch (Exception e1) {
        throw new RuntimeException(e);
      }
    } finally {
      resetDbLock.readLock().unlock();
    }
  }

  @Override
  public boolean flush() {
    return false;
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
  public org.tron.core.db.common.iterator.DBIterator iterator() {
    return new StoreIterator(database.newIterator());
  }

  public Stream<Entry<byte[], byte[]>> stream() {
    return StreamSupport.stream(spliterator(), false);
  }

  public Stream<Entry<byte[], byte[]>> parallelStream() {
    return StreamSupport.stream(spliterator(), true);
  }

}
