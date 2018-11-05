package org.tron.common.storage.leveldb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Sets;
import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.FileUtil;
import org.tron.core.Constant;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;

/**
 * @program: java-tron
 * @description:
 * @author: shydesky@gmail.com
 * @create: 2018-10-30
 **/

@Slf4j
public class RocksDbDataSourceImplTest {

  private static Application AppT;
  private static TronApplicationContext context;
  private static final String dbPath = "output-levelDb-test";
  private static RocksDbDataSourceImpl dataSourceTest;

  @Before
  public void initDb() {
    Args.setParam(new String[]{"--output-directory", dbPath}, Constant.TEST_CONF);
    //context = new TronApplicationContext(DefaultConfig.class);
    //AppT = ApplicationFactory.create(context);
    dataSourceTest = new RocksDbDataSourceImpl(dbPath + File.separator, "test_levelDb");
  }


  @After
  public void destroy() {
    Args.clearParam();
    dataSourceTest.closeDB();
    /*
    AppT.shutdownServices();
    AppT.shutdown();*/
    //context.destroy();
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.info("Release resources failure.");
    }
  }

  @Test
  public void testInitDB() {
    dataSourceTest.initDB();
    assertEquals(true, dataSourceTest.isAlive());
    dataSourceTest.closeDB();
  }

  @Test
  public void testCloseDB() {
    dataSourceTest.initDB();
    dataSourceTest.closeDB();
    assertEquals(false, dataSourceTest.isAlive());
  }

  @Test
  public void testReset() {
    dataSourceTest.initDB();
    dataSourceTest.putData("key".getBytes(), "value".getBytes());
    assertEquals(1, dataSourceTest.allKeys().size());
    dataSourceTest.resetDb();
    assertEquals(0, dataSourceTest.allKeys().size());
  }

  @Test
  public void testPutGet() {
    dataSourceTest.initDB();

    byte[] key1 = "key1".getBytes();
    byte[] value1 = "value1".getBytes();

    dataSourceTest.putData(key1, value1);

    assertNotNull(dataSourceTest.getData(key1));
    assertEquals("value1", ByteArray.toStr(dataSourceTest.getData(key1)));

    dataSourceTest.putData(key1, "value2".getBytes());
    assertEquals("value2", ByteArray.toStr(dataSourceTest.getData(key1)));
  }

  @Test
  public void testDeleteData() {
    dataSourceTest.initDB();
    byte[] key = "key".getBytes();
    byte[] value = "value".getBytes();
    dataSourceTest.putData(key, value);
    assertEquals(ByteArray.toStr(value), ByteArray.toStr(dataSourceTest.getData(key)));

    dataSourceTest.deleteData(key);
    byte[] valueAfterDelete = dataSourceTest.getData(key);

    String s = ByteArray.toStr(valueAfterDelete);
    assertNull(s);
  }

  @Test
  public void testAllKeys() {
    dataSourceTest.initDB();

    assertEquals(0, dataSourceTest.allKeys().size());

    byte[] key1 = "key1".getBytes();
    byte[] key2 = "key2".getBytes();
    byte[] key3 = "key3".getBytes();

    byte[] value1 = "value1".getBytes();
    byte[] value2 = "value2".getBytes();
    byte[] value3 = "value3".getBytes();

    dataSourceTest.putData(key1, value1);
    dataSourceTest.putData(key2, value2);
    dataSourceTest.putData(key3, value3);

    assertEquals(3, dataSourceTest.allKeys().size());
    ArrayList<String> keyString = new ArrayList<>();
    dataSourceTest.allKeys().forEach(
        key -> keyString.add(new String(key))
    );

    assertTrue(keyString.contains(ByteArray.toStr(key1)));
    assertTrue(keyString.contains(ByteArray.toStr(key1)));
    assertTrue(keyString.contains(ByteArray.toStr(key1)));
  }

  private void putSomeKeyValue() {
    byte[] value1 = "10000".getBytes();
    byte[] value2 = "20000".getBytes();
    byte[] value3 = "30000".getBytes();
    byte[] value4 = "40000".getBytes();
    byte[] value5 = "50000".getBytes();
    byte[] value6 = "60000".getBytes();
    byte[] key1 = "00000001aa".getBytes();
    byte[] key2 = "00000002aa".getBytes();
    byte[] key3 = "00000003aa".getBytes();
    byte[] key4 = "00000004aa".getBytes();
    byte[] key5 = "00000005aa".getBytes();
    byte[] key6 = "00000006aa".getBytes();

    dataSourceTest.putData(key1, value1);
    dataSourceTest.putData(key6, value6);
    dataSourceTest.putData(key2, value2);
    dataSourceTest.putData(key5, value5);
    dataSourceTest.putData(key3, value3);
    dataSourceTest.putData(key4, value4);
  }

  @Test
  public void testGetValuesNext() {
    dataSourceTest.initDB();
    dataSourceTest.putData("00000001aa".getBytes(), "10000".getBytes());
    dataSourceTest.putData("00000002aa".getBytes(), "20000".getBytes());
    dataSourceTest.putData("00000003aa".getBytes(), "30000".getBytes());
    dataSourceTest.putData("00000004aa".getBytes(), "40000".getBytes());

    Set<byte[]> seekKeyLimitNext = dataSourceTest.getValuesNext("0000000300".getBytes(), 2);
    Assert.assertEquals(2, seekKeyLimitNext.size());
    HashSet<String> hashSet = Sets.newHashSet("30000", "40000");
    seekKeyLimitNext.forEach(value -> {
      Assert.assertTrue(hashSet.contains(ByteArray.toStr(value)));
    });
  }

  @Test
  public void getValuesPrev() {
    dataSourceTest.resetDb();
    dataSourceTest.putData("00000001aa".getBytes(), "10000".getBytes());
    dataSourceTest.putData("00000002aa".getBytes(), "20000".getBytes());

    dataSourceTest.putData("00000003aa".getBytes(), "30000".getBytes());
    dataSourceTest.putData("00000004aa".getBytes(), "40000".getBytes());

    Set<byte[]> seekKeyLimitNext = dataSourceTest.getValuesPrev("0000000300".getBytes(), 2);
    Assert.assertEquals(2, seekKeyLimitNext.size());

    HashSet<String> hashSet = Sets.newHashSet("10000","20000");
    seekKeyLimitNext.forEach(value -> {
      Assert.assertTrue(hashSet.contains(ByteArray.toStr(value)));
    });
    seekKeyLimitNext = dataSourceTest.getValuesPrev("0000000100".getBytes(), 2);
    Assert.assertEquals(0, seekKeyLimitNext.size());
  }

}