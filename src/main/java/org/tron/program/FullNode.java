package org.tron.program;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.tron.common.application.Application;
import org.tron.common.application.ApplicationFactory;
import org.tron.core.Constant;
import org.tron.core.config.DefaultConfig;
import org.tron.core.config.args.Args;
import org.tron.core.services.RpcApiService;
import org.tron.core.services.WitnessService;
import org.tron.program.cat.StatsConsumer;
import org.tron.program.cat.StatsOnhandle;

import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class FullNode {

  /**
   * Start the FullNode.
   */
  public static void main(String[] args) throws InterruptedException {
    logger.info("Full node running.");
    Args.setParam(args, Constant.TESTNET_CONF);
    Args cfgArgs = Args.getInstance();

    if (cfgArgs.isHelp()) {
      logger.info("Here is the help message.");
      return;
    }
    
    ApplicationContext context = new AnnotationConfigApplicationContext(DefaultConfig.class);
    Application appT = ApplicationFactory.create(context);
    shutdown(appT);
    print();
    //appT.init(cfgArgs);
    RpcApiService rpcApiService = context.getBean(RpcApiService.class);
    appT.addService(rpcApiService);
    if (cfgArgs.isWitness()) {
      appT.addService(new WitnessService(appT));
    }
    appT.initServices(cfgArgs);
    appT.startServices();
    appT.startup();
    rpcApiService.blockUntilShutdown();
  }

  public static void shutdown(final Application app) {
    logger.info("********register application shutdown hook********");
    Runtime.getRuntime().addShutdownHook(new Thread(app::shutdown));
  }

  public static void print() {
    logger.info("***********************begin");
    StatsConsumer.service.scheduleAtFixedRate(() ->
            logger.info("*****net send tps:" + StatsConsumer.stats.keySet().stream()
                .sorted(Comparator.comparingLong((Long l) -> l).reversed())
                .limit(2)
                .map(key -> key + ":" + StatsConsumer.stats.get(key).get())
                .collect(Collectors.joining(";"))
            ),
        10, 5, TimeUnit.SECONDS);
    StatsOnhandle.service.scheduleAtFixedRate(() ->
            logger.info("*****net recive tps:" + StatsOnhandle.stats.keySet().stream()
                .sorted(Comparator.comparingLong((Long l) -> l).reversed())
                .limit(2)
                .map(key -> key + ":" + StatsConsumer.stats.get(key).get())
                .collect(Collectors.joining(";"))
            ),
        10, 5, TimeUnit.SECONDS);

  }
}
