package org.tron.program.cat;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.application.Service;
import org.tron.common.overlay.message.Message;
import org.tron.core.net.peer.PeerConnection;

import javax.print.attribute.standard.Finishings;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

@Slf4j
@Value(staticConstructor = "of")
public class StatsConsumer implements Consumer<PeerConnection> {
  private static final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
  private static final ConcurrentHashMap<Long, AtomicLong> stats = new ConcurrentHashMap<>();
  private Message message;

  static {
      service.scheduleAtFixedRate(() -> logger.info("*****net send tps:" + stats),
          10, 5, TimeUnit.SECONDS);
  }

  @Override
  public void accept(PeerConnection peerConnection) {
    peerConnection.sendMessage(message);
    stats.computeIfAbsent(System.currentTimeMillis()/1000L, AtomicLong::new).incrementAndGet();
  }
}
