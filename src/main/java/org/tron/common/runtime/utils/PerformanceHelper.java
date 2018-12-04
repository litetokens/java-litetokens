package org.tron.common.runtime.utils;

import java.util.ArrayList;
import java.util.List;

public class PerformanceHelper {

  private static PerformanceHelper _instance = new PerformanceHelper();

  public static ArrayList<ArrayList<String>> txBaseInfo = new ArrayList<ArrayList<String>>();
  public static ArrayList<ArrayList<String>> txOpcodeInfo = new ArrayList<ArrayList<String>>();
  public static ArrayList<ArrayList<String>> txCommitInfo = new ArrayList<ArrayList<String>>();
  public static List<String> blockInfo = new ArrayList<String>();
  public static int txIndex = -1;

  public static ArrayList<String> singleTxBaseInfo = new ArrayList<String>();
  public static ArrayList<String> singleTxOpcodeInfo = new ArrayList<String>();
  public static ArrayList<String> singleTxCommitInfo = new ArrayList<String>();

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
