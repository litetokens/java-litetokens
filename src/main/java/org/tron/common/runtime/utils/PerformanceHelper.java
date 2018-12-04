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
  public static ArrayList<ArrayList<String>> txCommitInfo = new ArrayList<ArrayList<String>>();
  public static ArrayList<ArrayList<String>> blockInfo = new ArrayList<ArrayList<String>>();
  public static int txIndex = -1;

  public static ArrayList<String> singleTxBaseInfo = new ArrayList<String>();
  public static ArrayList<String> singleTxOpcodeInfo = new ArrayList<String>();
  public static ArrayList<String> singleBlockInfo = new ArrayList<String>();
  public static ArrayList<String> singleTxCommitInfo = new ArrayList<String>();

  public static void writeList(List<String> l, String fileName) {

    String content = String.join("\t", l);
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

    try (Writer writer = new FileWriter(file)) {
      writer.write(content); // need "\n"?
    } catch (IOException e) {
      logger.info(e.getMessage());
    }
  }

  public static void write2DList(ArrayList<ArrayList<String>> l, String fileName) {
    String content = "";
    for (int i = 0; i < l.size(); i++) {
      content += String.join("\t", l.get(i)) + "\n";

    }
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

    try (Writer writer = new FileWriter(file)) {
      writer.write(content);
    } catch (IOException e) {
      logger.info(e.getMessage());
    }
  }

  // public void recordTxBaseInfo(String info, String parameter) {
  //
  //   long newTs = System.currentTimeMillis();
  //
  //   return ts;
  // }
  //
  // public void recordTxBaseInfo(String info, String parameter) {
  //
  //   long newTs = System.currentTimeMillis();
  //
  //   return ts;
  // }

}
