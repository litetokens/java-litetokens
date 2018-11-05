package org.tron.core.db.backup;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.config.args.Args;
import org.tron.core.db.AccountIdIndexStore;
import org.tron.core.db.AccountIndexStore;
import org.tron.core.db.AccountStore;
import org.tron.core.db.AssetIssueStore;
import org.tron.core.db.BlockIndexStore;
import org.tron.core.db.BlockStore;
import org.tron.core.db.CodeStore;
import org.tron.core.db.ContractStore;
import org.tron.core.db.DynamicPropertiesStore;
import org.tron.core.db.ExchangeStore;
import org.tron.core.db.ProposalStore;
import org.tron.core.db.RecentBlockStore;
import org.tron.core.db.StorageRowStore;
import org.tron.core.db.TransactionHistoryStore;
import org.tron.core.db.TransactionStore;
import org.tron.core.db.VotesStore;
import org.tron.core.db.WitnessScheduleStore;
import org.tron.core.db.WitnessStore;


@Slf4j
@Component
public class BackupDbUtil {

  public static final String DB_BACKUP_STATE = "DB";

  public enum STATE {
    UNDEFINED(0), BAKINGONE(1), BAKEDONE(11), BAKINGTWO(2), BAKEDTWO(22);

    public int status;

    private STATE(int status) {
      this.status = status;
    }

    public int getStatus() {
      return status;
    }

    public static STATE valueOf(int value) {
      switch (value) {
        case 1:
          return BAKINGONE;
        case 11:
          return BAKEDONE;
        case 2:
          return BAKINGTWO;
        case 22:
          return BAKEDTWO;
        default:
          return UNDEFINED;
      }
    }

  }

  @Autowired
  private AccountStore accountStore;
  @Autowired
  private TransactionStore transactionStore;
  @Autowired
  private BlockStore blockStore;
  @Autowired
  private WitnessStore witnessStore;
  @Autowired
  private AssetIssueStore assetIssueStore;
  @Autowired
  private DynamicPropertiesStore dynamicPropertiesStore;
  @Autowired
  private BlockIndexStore blockIndexStore;
  @Autowired
  private AccountIdIndexStore accountIdIndexStore;
  @Autowired
  private AccountIndexStore accountIndexStore;
  @Autowired
  private WitnessScheduleStore witnessScheduleStore;
  @Autowired
  private RecentBlockStore recentBlockStore;
  @Autowired
  private VotesStore votesStore;
  @Autowired
  private ProposalStore proposalStore;
  @Autowired
  private ExchangeStore exchangeStore;
  @Autowired
  private TransactionHistoryStore transactionHistoryStore;
  @Autowired
  private CodeStore codeStore;
  @Autowired
  private ContractStore contractStore;
  @Autowired
  private StorageRowStore storageRowStore;

  @Autowired
  private Args args;

  private String propPath;
  Properties prop = new Properties();
  InputStream in = null;


  @Getter
  @Setter
  private long currentBlockNum;

  public int backupState;

  public BackupDbUtil() {
    this.propPath = "/Users/tron/src/java-tron/src/main/java/org/tron/core/db/backup/a.properties";
  }

  public int getBackupState() {
    try {
      in = new BufferedInputStream(new FileInputStream(propPath));
      prop.load(in);
    } catch (IOException e) {
      logger.error("getBackupState:{}", e.getMessage());
    }
    return Integer.valueOf((String) prop.get(BackupDbUtil.DB_BACKUP_STATE));
  }

  public void setBackupState(int state) {
    try {
      FileOutputStream oFile = new FileOutputStream(propPath);
      prop.setProperty(BackupDbUtil.DB_BACKUP_STATE, String.valueOf(state));
      prop.store(oFile, "BackupDB properties file");
      oFile.close();
    } catch (IOException e) {
      logger.error("setBackupState:{}", e.getMessage());
    }
  }

  private void switchBackupState() throws UndefinedStateException {
    // switch to new state
    switch (STATE.valueOf(getBackupState())) {
      case BAKINGONE:
        setBackupState(STATE.BAKEDONE.getStatus());
        break;
      case BAKEDONE:
        setBackupState(STATE.BAKEDTWO.getStatus());
        break;
      case BAKINGTWO:
        setBackupState(STATE.BAKEDTWO.getStatus());
        break;
      case BAKEDTWO:
        setBackupState(STATE.BAKEDONE.getStatus());
        break;
      case UNDEFINED:
        throw new UndefinedStateException();
    }
  }

  public void doBackup() {
    long t1 = System.currentTimeMillis();

    try {
      switch (STATE.valueOf(getBackupState())) {
        case BAKINGONE:
          deleteDbBakPath(1);  //删除旧备份
          backup(1);
          switchBackupState(); //备份成功之后，记下完整的备份号。
          deleteDbBakPath(2);
          break;
        case BAKEDONE:
          deleteDbBakPath(2);
          backup(2);
          switchBackupState(); //备份成功之后，记下完整的备份号。
          deleteDbBakPath(1);
          break;
        case BAKINGTWO:
          deleteDbBakPath(2);  //删除旧备份
          backup(2);
          switchBackupState(); //备份成功之后，记下完整的备份号。
          deleteDbBakPath(1);
          break;
        case BAKEDTWO:
          deleteDbBakPath(1);
          backup(1);
          switchBackupState(); //备份成功之后，记下完整的备份号。
          deleteDbBakPath(2);
          break;
      }
    } catch (UndefinedStateException e) {
      e.printStackTrace();
    }
    logger.info("backupAllStore use {} ms!", System.currentTimeMillis() - t1);
  }

  private void backup(int i) {
    accountStore.backup(i);
    blockStore.backup(i);
    transactionStore.backup(i);
    witnessStore.backup(i);
    assetIssueStore.backup(i);
    dynamicPropertiesStore.backup(i);
    blockIndexStore.backup(i);
    accountIdIndexStore.backup(i);
    accountIndexStore.backup(i);
    witnessScheduleStore.backup(i);
    recentBlockStore.backup(i);
    votesStore.backup(i);
    proposalStore.backup(i);
    exchangeStore.backup(i);
    transactionHistoryStore.backup(i);
    codeStore.backup(i);
    contractStore.backup(i);
    storageRowStore.backup(i);
  }

  private void deleteDbBakPath(int i) {
    accountStore.deleteDbBakPath(i);
    blockStore.deleteDbBakPath(i);
    transactionStore.deleteDbBakPath(i);
    witnessStore.deleteDbBakPath(i);
    assetIssueStore.deleteDbBakPath(i);
    dynamicPropertiesStore.deleteDbBakPath(i);
    blockIndexStore.deleteDbBakPath(i);
    accountIdIndexStore.deleteDbBakPath(i);
    accountIndexStore.deleteDbBakPath(i);
    witnessScheduleStore.deleteDbBakPath(i);
    recentBlockStore.deleteDbBakPath(i);
    votesStore.deleteDbBakPath(i);
    proposalStore.deleteDbBakPath(i);
    exchangeStore.deleteDbBakPath(i);
    transactionHistoryStore.deleteDbBakPath(i);
    codeStore.deleteDbBakPath(i);
    contractStore.deleteDbBakPath(i);
    storageRowStore.deleteDbBakPath(i);
  }

}

class UndefinedStateException extends RuntimeException {

}