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
import java.text.DecimalFormat;
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
      int counter = 1;
      System.out.println("Read accounts file:");
      ConsolePrint consolePrint = new ConsolePrint();
      long startTime = System.currentTimeMillis();
      while (fis.available() > 0) {
        Account account = (Account) ois.readObject();
        accounts.add(account);
        consolePrint.show(counter, System.currentTimeMillis() - startTime);
        counter++;
      }

    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }

    System.out.println();
    // 开始存
    int counter = 1;
    System.out.println("Save accounts:");
    ConsolePrint consolePrint = new ConsolePrint(counter, accounts.size());
    long startTime = System.currentTimeMillis();
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
      long useTime = System.currentTimeMillis() - startTime;
      long remainTime = (accounts.size() - counter) * useTime / counter;
      consolePrint.show(counter, accounts.size(), useTime, remainTime);
      counter++;
    }

    System.out.println("Completed!");
    System.exit(0);
  }
}

class ConsolePrint {
  private long startBlockNumber;
  private long endBlockNumber;
  private long currentBlockNumber;
  private long tipsLength = 20;
  private char showTips = '>';
  private char hiddenTips = '-';
  private DecimalFormat formater = new DecimalFormat("0.00%");

  public ConsolePrint(long startBlockNumber, long endBlockNumber) {
    this.startBlockNumber = startBlockNumber;
    this.endBlockNumber = endBlockNumber;
  }

  public ConsolePrint() {

  }

  public void show(long value, long total, long useTime, long remainTime) {
    if (value < startBlockNumber || value > endBlockNumber) {
      return;
    }

    System.out.print('\r');
    currentBlockNumber = value;
    float rate = (float) (currentBlockNumber * 1.0 / endBlockNumber);
    long len = (long) (rate * tipsLength);
    draw(len, rate, value, total, useTime, remainTime);
    if (currentBlockNumber == endBlockNumber) {
      System.out.println();
    }
  }

  private void draw(long len, float rate, long value, long total, long useTime, long remainTime) {
    System.out.print("Progress: ");
    for (int i = 0; i < len; i++) {
      System.out.print(showTips);
    }

    for (long i = len; i < tipsLength; i++) {
      System.out.print(hiddenTips);
    }

    System.out.print(' ');
    System.out.print(formater.format(rate));
    System.out.print(" Current: " + value);
    System.out.print(" Total: " + total);
    System.out.print(" Time: " + (useTime / 1000) + "s");
    System.out.print(" Remain: " + (remainTime / 1000) + "s");
  }

  public void show(long value, long useTime) {
    System.out.print('\r');
    currentBlockNumber = value;
    draw(value, useTime);
  }

  private void draw(long value, long useTime) {
    System.out.print("Progress: ");
    System.out.print(' ');
    System.out.print(" Current: " + value);
    System.out.print(" Time: " + (useTime / 1000) + "s");
  }
}