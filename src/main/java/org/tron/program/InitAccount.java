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
package org.tron.program;

import com.google.protobuf.ByteString;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.tron.common.utils.ByteArray;
import org.tron.core.Constant;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.AccountStore;
import org.tron.core.db.Manager;
import org.tron.module.Account;
import org.tron.protos.Protocol;

public class InitAccount {
  public static List<Account> accounts = new ArrayList<>();

  public static void main(String[] args) {
    Args.setParam(args, Constant.TESTNET_CONF);

    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(DefaultConfig.class);
    Manager dbManager = context.getBean(Manager.class);

    File ff = new File("accounts.txt");
    FileInputStream fis = null;
    try {
      fis = new FileInputStream(ff);
      ObjectInputStream ois = new ObjectInputStream(fis);
      int counter = 0;
      while (fis.available() > 0) {
        Account account = (Account) ois.readObject();
        accounts.add(account);
        if ((counter + 1) % 100000 == 0) {
          System.out.println("read account current: " + (counter + 1));
        }
        counter++;
      }

    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }

    // 开始存
    int counter = 0;

    for (int index = 0; index < accounts.size(); index++) {
      String addressBase58 = accounts.get(index).getAddress();
      byte[] addressBytes = ByteArray.fromHexString(addressBase58);
      ByteString addressByteString = ByteString.copyFrom(addressBytes);
      AccountStore accountStore = dbManager.getAccountStore();
      ByteString name = ByteString.copyFrom(ByteArray.fromString("test" + index));

      AccountCapsule accountCapsule = new AccountCapsule(
          Protocol.Account.newBuilder()
              .setAccountName(name)
              .setAddress(addressByteString)
              .setBalance(1_000_000_000)
              .build()
      );

      accountStore.put(addressBytes, accountCapsule);

      if ((counter + 1) % 100000 == 0) {
        System.out.println("save account current: " + (counter + 1));
      }
      counter++;
    }

    System.exit(0);
  }
}