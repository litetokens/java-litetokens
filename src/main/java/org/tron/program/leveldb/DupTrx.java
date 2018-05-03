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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.tron.core.Constant;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.db.BlockStore;

public class DupTrx {

  public static void main(String[] args) {
    Args.setParam(args, Constant.TESTNET_CONF);
    BlockStore blockStore = BlockStore.create("block");

    Map<String, List<Long>> result = getResult(blockStore);

    result.entrySet().stream().filter(r -> r.getValue().size() > 1).forEach(r -> {
      System.out.println(r.getKey() + ": " +  r.getValue());
    });
  }

  public static Map<String, List<Long>> getResult(BlockStore blockStore) {
    Iterator<Entry<byte[], BlockCapsule>> iterator = blockStore.iterator();

    Map<String, List<Long>> result = new HashMap<>();

    while (iterator.hasNext()) {
      Entry<byte[], BlockCapsule> next = iterator.next();

      BlockCapsule value = next.getValue();
      long num = value.getNum();

      List<TransactionCapsule> transactions = value.getTransactions();

      transactions.stream().forEach(t -> {
        List<Long> longs = result.get(t.getTransactionId().toString());

        if (null == longs) {
          longs = new ArrayList<>();
        }

        longs.add(num);
        result.put(t.getTransactionId().toString(), longs);
      });
    }

    return result;
  }
}
