package org.tron.common.runtime.utils;

import lombok.extern.slf4j.Slf4j;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class PerformanceHelper {

  private static PerformanceHelper _instance = new PerformanceHelper();

  public static ArrayList<ArrayList<String>> txBaseInfo = new ArrayList<ArrayList<String>>();
  public static ArrayList<ArrayList<String>> txOpcodeInfo = new ArrayList<ArrayList<String>>();
  public static ArrayList<ArrayList<String>> txGetPutInfo = new ArrayList<ArrayList<String>>();
  public static ArrayList<ArrayList<String>> blockInfo = new ArrayList<ArrayList<String>>();
  public static int txIndex = -1;

  public static ArrayList<String> singleTxBaseInfo = new ArrayList<String>();
  public static ArrayList<String> singleTxOpcodeInfo = new ArrayList<String>();
  public static ArrayList<String> singleBlockInfo = new ArrayList<String>();
  public static ArrayList<String> singleTxGetPutInfo = new ArrayList<String>();


  public static void write2DList(ArrayList<ArrayList<String>> l, String fileName) {
    StringBuffer content = new StringBuffer();
    // logger.info("length of 2D list: {}", l.size());
    for (int i = 0; i < l.size(); i++) {
      // long preMs = System.nanoTime() / 1000;
      String c = String.join("\t", l.get(i));
      // long now = System.nanoTime() / 1000;
      // logger.info("index:{}, join consume: {}", i, now - preMs);
      // preMs = now;
      content.append(c + "\n");
      // now = System.nanoTime() / 1000;
      // logger.info("index:{}, append consume: {}", i, now - preMs);
    }
    // logger.info("length of 2D list: {}", l.size());
    File file = new File(fileName);

    try {
      if (!file.getParentFile().exists() && !file.isDirectory()) {
        file.getParentFile().mkdirs();
        file.createNewFile();
      } else {
        file.createNewFile();
      }
    } catch (IOException e) {
      logger.info(e.getMessage());
    }

    // long now = System.nanoTime() / 1000;
    try (Writer writer = new FileWriter(file)) {
      writer.write(content.toString());
    } catch (IOException e) {
      logger.info(e.getMessage());
    }
    // logger.info("write consume: {}", System.nanoTime() / 1000 - now);

  }

}
