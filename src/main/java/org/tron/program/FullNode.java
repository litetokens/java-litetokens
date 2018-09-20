package org.tron.program;

import ch.qos.logback.classic.Level;
import com.beust.jcommander.internal.Lists;
import com.google.protobuf.ByteString;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.common.application.TronApplicationContext;
import org.tron.common.utils.ByteArray;
import org.tron.core.Constant;
import org.tron.core.Wallet;
import org.tron.core.capsule.AccountCapsule;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;
import org.tron.core.services.RpcApiService;
import org.tron.core.services.WitnessService;
import org.tron.core.services.http.FullNodeHttpApiService;
import org.tron.core.witness.WitnessController;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.DynamicProperties;

@Slf4j
public class FullNode {

  /**
   * Start the FullNode.
   */
  public static void main(String[] args) throws InterruptedException {
    logger.info("Full node running.");
    Args.setParam(args, Constant.TESTNET_CONF);
    Args cfgArgs = Args.getInstance();

    ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    root.setLevel(Level.toLevel(cfgArgs.getLogLevel()));

    if (cfgArgs.isHelp()) {
      logger.info("Here is the help message.");
      return;
    }

    if (Args.getInstance().isDebug()) {
      logger.info("in debug mode, it won't check energy time");
    } else {
      logger.info("not in debug mode, it will check energy time");
    }

    DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
    beanFactory.setAllowCircularReferences(false);

    TronApplicationContext context =
        new TronApplicationContext(beanFactory);
    context.register(DefaultConfig.class);
    context.refresh();
    Application appT = ApplicationFactory.create(context);
//
    mockWitness(context);

    shutdown(appT);

    // grpc api server
    RpcApiService rpcApiService = context.getBean(RpcApiService.class);
    appT.addService(rpcApiService);
    if (cfgArgs.isWitness()) {
      appT.addService(new WitnessService(appT, context));
    }

    // http api server
    FullNodeHttpApiService httpApiService = context.getBean(FullNodeHttpApiService.class);
    appT.addService(httpApiService);

    appT.initServices(cfgArgs);
    appT.startServices();
    appT.startup();

    rpcApiService.blockUntilShutdown();
  }

  private static void mockWitness(TronApplicationContext context) {
    Manager manager = context.getBean(Manager.class);
    manager.getWitnessStore().getAllWitnesses().forEach(witnessCapsule -> {
      manager.getWitnessStore().delete(witnessCapsule.getAddress().toByteArray());
    });
    String[] newAccount = {
        "TRx2rc1v91HjUFdeBBgNSiqirctq94sAfA",
        "TRxETQim3Jn5TYqLeAnpyF5XdQeg7NUcSJ",
        "TRxUu1ZhEYsZw9AHyg8gXBmRSmUzaZPWaw",
        "TRxgBU7HFTQvU6zPheLHphqpwhDKNxB6Rk",
        "TRxscEvPTPFaCxBMuFVmzEybzZWJZM9eAB",
        "TRx32uh7TQjdnLFKyWVPKJBfEn1XWjJtcm",
        "TRxF8fZERk4XzQZe1SzvkS5nyNJ7x6tGZ5",
        "TRxUztFKWdXy42MSdiHQoef5VLaXADMJp3",
        "TRxh1GnspMRadaU37UzrRRpkME2EkwCHg4",
        "TRxsiQ2vugWqY2JGr39NHqysAw5zHfWhpU",
        "TRx3MZDxWzTBW3HYX3ZWGEBrvAC8upGA8C",
        "TRxFANjAvztBibiqPRWgG841fVP12BCH7d",
        "TRxVs5MRUy2yHn2kqwev81VjYwXBdYdXrD",
        "TRxhePptGctYfCpxFCsLLAHUr1iShFkGC1",
        "TRxtaoGBJeSwQJu5551cBhaw5sW3vaazuF",
        "TRx3cWa892UxbCaoqCjidp3r946SLZ6U72",
        "TRxFZ7TDgQGF8MfLxnjQ9EqL5WtEiUmTmH",
        "TRxVyqGWNwiPCetP7EQTnukdsVGgMpXzwj",
        "TRxinhH2wZa4zPCqgcUgEZTx3uYs9bFKuM",
        "TRxtfixDf8e4MnZw6zRAVbL3isVnnaiq2o",
        "TRx4sTiyZuDN8whJUyovHZNTk6UYdsqqwg",
        "TRxFiLJp8i5YMQyG2rJFzNA9htaTc7wLcf",
        "TRxXnVabXh8QzdPvAGigmyuYuC391hzmwL",
        "TRxiyR3cJPwyMMpq3WQQF7xiRkNDLkyd9X",
        "TRxu36iquybaSti8ZhVzZ2tPgK7NiXTrSn",
        "TRx4znAxu5FWxb5ccVUX89TtZ8qWF2PM2b",
        "TRxYCcQNn7U7RtN7ZqF36GQYhfMTKnoarw"
    };

    int idx = 0;
    for (String acc: newAccount) {
      byte[] address = Wallet.decodeFromBase58Check(acc);
      AccountCapsule account = new AccountCapsule(ByteString.copyFrom(address), AccountType.Normal);
      account.setBalance(9000000000000000000L);
      account.addVotes(ByteString.copyFrom(address), 1_000_000_000_000L);
      account.setFreeNetUsage(1_000_000_000_000L);
      account.setFrozen(1_000_000_000_000L, 10000);
      context.getBean(Manager.class).getAccountStore().put(address, account);
      manager.insertWitness(address, idx++);
    }
    manager.getWitnessController().initWits();

    manager.getDynamicPropertiesStore().saveMaintenanceTimeInterval(600000);
  }

  public static void shutdown(final Application app) {
    logger.info("********register application shutdown hook********");
    Runtime.getRuntime().addShutdownHook(new Thread(app::shutdown));
  }
}
