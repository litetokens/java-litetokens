package org.tron.core.db;

/**
 * @program: java-tron
 * @description:
 * @author: shydesky@gmail.com
 * @create: 2018-11-06
 **/


public class RocksRevokingStore extends AbstractRevokingStore {

  public RocksRevokingStore() {
  }

  public static RocksRevokingStore getInstance() {
    return RevokingEnum.INSTANCE.getInstance();
  }

  private enum RevokingEnum {
    INSTANCE;

    private RocksRevokingStore instance;

    RevokingEnum() {
      instance = new RocksRevokingStore();
    }

    private RocksRevokingStore getInstance() {
      return instance;
    }
  }
}