package org.litetokens.common.application;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.litetokens.common.overlay.discover.DiscoverServer;
import org.litetokens.common.overlay.discover.node.NodeManager;
import org.litetokens.common.overlay.server.ChannelManager;
import org.litetokens.core.db.Manager;

public class LitetokensApplicationContext extends AnnotationConfigApplicationContext {

  public LitetokensApplicationContext() {
  }

  public LitetokensApplicationContext(DefaultListableBeanFactory beanFactory) {
    super(beanFactory);
  }

  public LitetokensApplicationContext(Class<?>... annotatedClasses) {
    super(annotatedClasses);
  }

  public LitetokensApplicationContext(String... basePackages) {
    super(basePackages);
  }

  @Override
  public void destroy() {

    Application appT = ApplicationFactory.create(this);
    appT.shutdownServices();
    appT.shutdown();

    DiscoverServer discoverServer = getBean(DiscoverServer.class);
    discoverServer.close();
    ChannelManager channelManager = getBean(ChannelManager.class);
    channelManager.close();
    NodeManager nodeManager = getBean(NodeManager.class);
    nodeManager.close();
    
    Manager dbManager = getBean(Manager.class);
    dbManager.stopRepushThread();

    super.destroy();
  }
}
