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

import java.io.File;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.tron.common.utils.FileUtil;
import org.tron.core.Constant;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;

public class AccountTest {
  private static Manager dbManager;
  private static AnnotationConfigApplicationContext context;
  private static String dbPath = "output_account_test";

  @Test
  public void testGetBalance() {
    Args.setParam(new String[] {"-d", dbPath}, Constant.TESTNET_CONF);

    context = new AnnotationConfigApplicationContext(DefaultConfig.class);

    dbManager = context.getBean(Manager.class);

    Assert.assertEquals("10000000000000000",
        "" + Account.getBalance("27d3byPxZXKQWfXX7sJvemJJuv5M65F3vjS",
            dbManager.getAccountStore()));
  }

  /**
   * remove db.
   */
  @AfterClass
  public static void removeDb() {
    Args.clearParam();
    FileUtil.deleteDir(new File(dbPath));
    context.destroy();
  }

}
