/*
 * java-tron is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * java-tron is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.core.db;

import org.apache.commons.lang3.RandomUtils;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.rocksdb.*;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.tron.common.utils.FileUtil;
import org.tron.core.Constant;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * DynamicPropertiesStoreTest
 *
 * @author haoyouqiang
 * @version 1.0
 * @since 2018/5/29
 */
@Ignore
public class DynamicPropertiesStoreTest {

  private static String dbPath = "output_DynamicPropertiesStore_test";
  private static AnnotationConfigApplicationContext context;
  private static DynamicPropertiesStore dynamicPropertiesStore;
  private static DB database;

  static {
    Args.setParam(
        new String[]{"--output-directory", dbPath},
        Constant.TEST_CONF
    );
    context = new AnnotationConfigApplicationContext(DefaultConfig.class);
  }

  @BeforeClass
  public static void init() {
    dynamicPropertiesStore = context.getBean(DynamicPropertiesStore.class);
//    database = dynamicPropertiesStore.getDbSource().getDatabase();
//
//    System.out.println(database.getProperty("leveldb.num-files-at-level0"));
//    System.out.println(database.getProperty("leveldb.stats"));
//    System.out.println(database.getProperty("leveldb.sstables"));
//    System.out.println(database.getProperty("leveldb.approximate-memory-usage"));
  }

  //  @AfterClass
  public static void destroy() {
    Args.clearParam();
    FileUtil.deleteDir(new File(dbPath));
    context.destroy();
  }

  @Test
  public void doNothing() {
    System.out.println(dynamicPropertiesStore.getFreeNetLimit());
  }

  @Test
  public void testWrite() {
    long sum = 0;
    for (int i = 0; i < 1; i++) {
      long start = System.currentTimeMillis();
      for (int j = 0; j < 1000000; j++) {
        database.put(String.valueOf(j).getBytes(), String.valueOf(j).getBytes());
      }
      sum += System.currentTimeMillis() - start;
    }
    System.out.println(sum / 10);

    System.out.println(database.getProperty("leveldb.sstables"));

    testRead();
  }

  @Test
  public void testRead() {

    int[] randomKeys = new int[1000000];
    for (int i = 0; i < 1000000; i++) {
      randomKeys[i] = RandomUtils.nextInt(0, 1000000);
    }

    long sum = 0;
    for (int i = 0; i < 10; i++) {
      long start = System.currentTimeMillis();
      for (int j = 0; j < 1000000; j++) {
        database.get(String.valueOf(randomKeys[j]).getBytes());
      }
      long interval = System.currentTimeMillis() - start;
      System.out.println(interval);

      sum += interval;
    }
    System.out.println(sum / 10);

  }

  @Test
  public void testWriteAndRead() throws IOException {
    Options options = new Options();
    options.writeBufferSize(10485760);
    options.cacheSize(10485760);
    options.blockSize(1);
    options.compressionType(CompressionType.SNAPPY);

    DB localDatabase = JniDBFactory.factory.open(new File("local_database"), options);
    System.out.println(localDatabase.getProperty("leveldb.sstables"));

    long start = System.currentTimeMillis();
//    for (int i = 0; i < 1000000; i++) {
//      localDatabase.put(String.valueOf(i).getBytes(), String.valueOf(i).getBytes());
//    }
//    System.out.println("Write: " + (System.currentTimeMillis() - start));
//
//    start = System.currentTimeMillis();
    for (int i = 0; i < 1000000; i++) {
      localDatabase.get(String.valueOf(i).getBytes());
    }
    System.out.println("Read 1: " + (System.currentTimeMillis() - start));

    start = System.currentTimeMillis();
    for (int i = 0; i < 1000000; i++) {
      localDatabase.get(String.valueOf(i).getBytes());
    }
    System.out.println("Read 2: " + (System.currentTimeMillis() - start));

    localDatabase.close();
  }

  public void backup(String from, String to) throws IOException {
    File[] files = new File(from).listFiles();
    if (files == null) {
      return;
    }

    File toDirectory = new File(to);
    if (!toDirectory.exists()) {
      toDirectory.mkdirs();
    }

    for (File file : files) {
      if (!file.isFile()) {
        continue;
      }

      String fileName = file.getName();
      Path fromPath = Paths.get(from, fileName);
      Path toPath = Paths.get(to, fileName);

      if (fileName.endsWith(".sst")) {
        Files.createLink(toPath, fromPath);
      } else {
        Files.copy(fromPath, toPath);
      }

    }

  }

  @Test
  public void testDbBackup() throws IOException {
    Options options = new Options();
    DB db = JniDBFactory.factory.open(new File("database"), options);
    db.put("1".getBytes(), "1".getBytes());
    backup("database", "database_copy");

    DB dbCopy = JniDBFactory.factory.open(new File("database_copy"), options);
    System.out.println(new String(dbCopy.get("1".getBytes())));

    System.out.println(dbCopy.getProperty("leveldb.num-files-at-level0"));
    System.out.println(dbCopy.getProperty("leveldb.stats"));
    System.out.println(dbCopy.getProperty("leveldb.sstables"));
    System.out.println(dbCopy.getProperty("leveldb.approximate-memory-usage"));
  }

  @Test
  public void testRocksDbBackup() {

    // a static method that loads the RocksDB C++ library.
    RocksDB.loadLibrary();

    // the Options class contains a set of configurable DB options
    // that determines the behaviour of the database.
    try (final org.rocksdb.Options options = new org.rocksdb.Options().setCreateIfMissing(true)) {

      // a factory method that returns a RocksDB instance
      try (final RocksDB db = RocksDB.open(options, "rocks_db")) {
//        for (int i = 0; i < 1000000; i++) {
//          db.put(String.valueOf(i).getBytes(), String.valueOf(i).getBytes());
//        }

        System.out.println(db.getProperty("rocksdb.dbstats"));

        long sum = 0;
        for (int i = 0; i < 10; i++) {
          long start = System.currentTimeMillis();
          Checkpoint.create(db).createCheckpoint("rocks_cp");
          sum += System.currentTimeMillis() - start;
          FileUtil.deleteDir(new File("rocks_cp"));
        }
        System.out.println(sum / 10);

//        try (BackupEngine backupEngine = BackupEngine.open(
//            Env.getDefault(),
//            new BackupableDBOptions("rocks_backup").setDestroyOldData(true))) {
//          long sum = 0;
//          for (int i = 0; i < 10; i++) {
//            long start = System.currentTimeMillis();
//            backupEngine.createNewBackup(db);
//            sum += System.currentTimeMillis() - start;
//          }
//          System.out.println(sum / 10);
//        }

      }

    } catch (RocksDBException e) {
      e.printStackTrace();
    }

  }

  @Test
  public void testRocksDbRestore() {

    RocksDB.loadLibrary();

    try (final RestoreOptions options = new RestoreOptions(false)) {
      try (BackupEngine backupEngine = BackupEngine.open(
          Env.getDefault(),
          new BackupableDBOptions("rocks_backup").setDestroyOldData(false))) {
        backupEngine.restoreDbFromLatestBackup("rocks_restore", "rocks_restore", options);
      }
    } catch (RocksDBException e) {
      e.printStackTrace();
    }

//    try (final org.rocksdb.Options options = new org.rocksdb.Options().setCreateIfMissing(true)) {
//      try (final RocksDB db = RocksDB.open(options, "rocks_restore")) {
//        System.out.println(db.getProperty("rocksdb.dbstats"));
//        System.out.println(new String(db.get("1".getBytes())));
//      }
//    } catch (RocksDBException e) {
//      e.printStackTrace();
//    }

  }

}