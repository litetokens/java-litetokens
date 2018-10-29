package org.tron.core.services.http;

import org.junit.Test;
import org.testng.Assert;

public class UtilTest {
  @Test
  public void testPrintHashMsg() {
    String hashStr = "0000000000000000000000000000000000000000000000000000000000000000";
    String hashMsg = Util.printHashMsg(hashStr);
    Assert.assertEquals(
        "{\"hash\":\"0000000000000000000000000000000000000000000000000000000000000000\"}",
        hashMsg);
  }
}
