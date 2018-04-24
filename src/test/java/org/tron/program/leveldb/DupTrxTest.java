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
package org.tron.program.leveldb;

import com.google.protobuf.ByteString;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.AfterClass;
import org.junit.Test;
import org.tron.common.utils.FileUtil;
import org.tron.core.Constant;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.db.BlockStore;
import org.tron.protos.Protocol.Transaction;

public class DupTrxTest {
  private static String dbPath = "output_dup_trx_test";
  @Test
  public void dup() {
    Args.setParam(new String[] {"-d", dbPath}, Constant.TESTNET_CONF);
    BlockStore blockStore = BlockStore.create("block");


    List<Transaction> transactions = new ArrayList<>();
    transactions.add(Transaction.getDefaultInstance());

    BlockCapsule blockCapsule1 = new BlockCapsule(1L, ByteString.EMPTY, 1L, transactions);
    BlockCapsule blockCapsule2 = new BlockCapsule(2L, ByteString.EMPTY, 2L, transactions);

    blockStore.getDbSource().putData(blockCapsule1.getBlockId().getBytes(), blockCapsule1.getData());
    blockStore.getDbSource().putData(blockCapsule2.getBlockId().getBytes(), blockCapsule2.getData());

    Map<String, List<Long>> result = DupTrx.getResult(blockStore);

    result.entrySet().stream().filter(r -> r.getValue().size() > 1).forEach(r -> {
      System.out.println(r.getKey() + ": " +  r.getValue());
    });
  }

  @AfterClass
  public static void removeDb() {
    Args.clearParam();
    FileUtil.deleteDir(new File(dbPath));
  }
}
