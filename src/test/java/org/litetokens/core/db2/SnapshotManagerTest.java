package org.litetokens.core.db2;

import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.litetokens.common.application.Application;
import org.litetokens.common.application.ApplicationFactory;
import org.litetokens.common.application.LitetokensApplicationContext;
import org.litetokens.common.storage.leveldb.LevelDbDataSourceImpl;
import org.litetokens.common.utils.FileUtil;
import org.litetokens.core.Constant;
import org.litetokens.core.config.DefaultConfig;
import org.litetokens.core.config.args.Args;
import org.litetokens.core.db2.RevokingDbWithCacheNewValueTest.TestRevokingLitetokensStore;
import org.litetokens.core.db2.RevokingDbWithCacheNewValueTest.TestSnapshotManager;
import org.litetokens.core.db2.SnapshotRootTest.ProtoCapsuleTest;
import org.litetokens.core.db2.core.ISession;
import org.litetokens.core.db2.core.SnapshotManager;
import org.litetokens.core.exception.BadItemException;
import org.litetokens.core.exception.ItemNotFoundException;

@Slf4j
public class SnapshotManagerTest {

  private SnapshotManager revokingDatabase;
  private LitetokensApplicationContext context;
  private Application appT;
  private TestRevokingLitetokensStore litetokensDatabase;

  @Before
  public void init() {
    Args.setParam(new String[]{"-d", "output_revokingStore_test"},
        Constant.TEST_CONF);
    context = new LitetokensApplicationContext(DefaultConfig.class);
    appT = ApplicationFactory.create(context);
    revokingDatabase = new TestSnapshotManager();
    revokingDatabase.enable();
    litetokensDatabase = new TestRevokingLitetokensStore("testSnapshotManager-test");
    revokingDatabase.add(litetokensDatabase.getRevokingDB());
    LevelDbDataSourceImpl tmpLevelDbDataSource  =
      new LevelDbDataSourceImpl(Args.getInstance().getOutputDirectoryByDbName("testSnapshotManager-tmp"), "testSnapshotManagerTmp");
    tmpLevelDbDataSource.initDB();
    revokingDatabase.setTmpLevelDbDataSource(tmpLevelDbDataSource);
  }

  @After
  public void removeDb() {
    Args.clearParam();
    appT.shutdownServices();
    appT.shutdown();
    context.destroy();
    litetokensDatabase.close();
    FileUtil.deleteDir(new File("output_revokingStore_test"));
    revokingDatabase.getTmpLevelDbDataSource().closeDB();
    litetokensDatabase.close();
  }

  @Test
  public synchronized void testRefresh()
      throws BadItemException, ItemNotFoundException {
    while (revokingDatabase.size() != 0) {
      revokingDatabase.pop();
    }

    revokingDatabase.setMaxFlushCount(0);
    revokingDatabase.setUnChecked(false);
    revokingDatabase.setMaxSize(5);
    ProtoCapsuleTest protoCapsule = new ProtoCapsuleTest("refresh".getBytes());
    for (int i = 1; i < 11; i++) {
      ProtoCapsuleTest testProtoCapsule = new ProtoCapsuleTest(("refresh" + i).getBytes());
      try (ISession tmpSession = revokingDatabase.buildSession()) {
        litetokensDatabase.put(protoCapsule.getData(), testProtoCapsule);
        tmpSession.commit();
      }
    }

    revokingDatabase.flush();
    Assert.assertEquals(new ProtoCapsuleTest("refresh10".getBytes()),
        litetokensDatabase.get(protoCapsule.getData()));
  }

  @Test
  public synchronized void testClose() {
    while (revokingDatabase.size() != 0) {
      revokingDatabase.pop();
    }

    revokingDatabase.setMaxFlushCount(0);
    revokingDatabase.setUnChecked(false);
    revokingDatabase.setMaxSize(5);
    ProtoCapsuleTest protoCapsule = new ProtoCapsuleTest("close".getBytes());
    for (int i = 1; i < 11; i++) {
      ProtoCapsuleTest testProtoCapsule = new ProtoCapsuleTest(("close" + i).getBytes());
      try (ISession _ = revokingDatabase.buildSession()) {
        litetokensDatabase.put(protoCapsule.getData(), testProtoCapsule);
      }
    }
    Assert.assertEquals(null,
        litetokensDatabase.get(protoCapsule.getData()));

  }
}
