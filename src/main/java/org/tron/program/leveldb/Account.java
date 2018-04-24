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

import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.config.args.Args;
import org.tron.core.db.AccountStore;

public class Account {


  /**
   * Start program.
   */
  public static void main(String[] args) {
    Args.setParam(args, Constant.TESTNET_CONF);
    AccountStore accountStore = AccountStore.create("account");

    String address = Args.getInstance().getAddress();

    System.out.println(getBalance(address, accountStore));
  }

  /**
   * Get balance.
   * @param address account address.
   * @param accountStore account store.
   * @return
   */
  public static long getBalance(String address, AccountStore accountStore) {

    byte[] key = Wallet.decodeFromBase58Check(address);

    if (!Wallet.addressValid(key)) {
      System.out.println("address is invalid");
      return -1;
    }

    AccountCapsule accountCapsule = accountStore.get(key);

    return accountCapsule.getBalance();
  }
}
