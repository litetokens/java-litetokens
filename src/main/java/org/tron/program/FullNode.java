package org.tron.program;

import com.google.common.collect.Streams;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.application.Application;
import org.tron.core.Constant;
import org.tron.core.config.args.Args;
import org.tron.core.db.AccountIdIndexStore;
import org.tron.core.db.AccountIndexStore;
import org.tron.core.db2.common.Base58;

@Slf4j
public class FullNode {

  /**
   * Start the FullNode.
   */
  public static void main(String[] args) throws InterruptedException {
    logger.info("Full node running.");
    Args.setParam(args, Constant.TESTNET_CONF);
    Args cfgArgs = Args.getInstance();

//    ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
//    root.setLevel(Level.toLevel(cfgArgs.getLogLevel()));
//
//    if (cfgArgs.isHelp()) {
//      logger.info("Here is the help message.");
//      return;
//    }
//
//    if (Args.getInstance().isDebug()) {
//      logger.info("in debug mode, it won't check energy time");
//    } else {
//      logger.info("not in debug mode, it will check energy time");
//    }
//
//    DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
//    beanFactory.setAllowCircularReferences(false);
//    TronApplicationContext context =
//        new TronApplicationContext(beanFactory);
//    context.register(DefaultConfig.class);
//
//    context.refresh();
//    Application appT = ApplicationFactory.create(context);
//    shutdown(appT);

    AccountIdIndexStore accountIdIndexStore = new AccountIdIndexStore("accountid-index");
//    DynamicPropertiesStore dynamicPropertiesStore = context.getBean(DynamicPropertiesStore.class);
    AccountIndexStore accountIndexStore = new AccountIndexStore("account-index");

    System.out.println(Streams.stream(accountIndexStore).count());
    Streams.stream(accountIndexStore)
        .sorted((e1, e2) -> ByteString.copyFrom(e1.getKey()).toStringUtf8()
            .compareToIgnoreCase(ByteString.copyFrom(e2.getKey()).toStringUtf8()))
        .map(e ->
            ByteString.copyFrom(e.getKey()).toStringUtf8() + "->" + Base58
                .encode58Check(e.getValue().getData()))
        .forEach(System.out::println);

    System.out.println("-------------");
    System.out.println(Streams.stream(accountIdIndexStore).count());
    Streams.stream(accountIdIndexStore).map(e ->
        ByteString.copyFrom(e.getKey()).toStringUtf8() + "->" + Base58
            .encode58Check(e.getValue().getData()))
        .forEach(System.out::println);
    System.exit(0);
//    System.out.println(Streams.stream(accountIdIndexStore).map(e ->
//        ByteString.copyFrom(e.getKey()).toStringUtf8()).collect(Collectors.toList()));

//    // grpc api server
//    RpcApiService rpcApiService = context.getBean(RpcApiService.class);
//    appT.addService(rpcApiService);
//    if (cfgArgs.isWitness()) {
//      appT.addService(new WitnessService(appT, context));
//    }
//
//    // http api server
//    FullNodeHttpApiService httpApiService = context.getBean(FullNodeHttpApiService.class);
//    appT.addService(httpApiService);
//
//    appT.initServices(cfgArgs);
//    appT.startServices();
//    appT.startup();
//
//    rpcApiService.blockUntilShutdown();
  }

  public static void shutdown(final Application app) {
    logger.info("********register application shutdown hook********");
    Runtime.getRuntime().addShutdownHook(new Thread(app::shutdown));
  }
}
