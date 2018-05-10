package org.tron.program.cat;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.application.Service;
import org.tron.common.overlay.message.Message;
import org.tron.common.utils.JMonitor;
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
  public static final long ONE_MINUTE = 60 * 1000L;
  public static final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
  public static final ConcurrentHashMap<Long, AtomicLong> stats = new ConcurrentHashMap<>();
  private Message message;

  @Override
  public void accept(PeerConnection peerConnection) {
    stats.computeIfAbsent(System.currentTimeMillis() / ONE_MINUTE, k -> new AtomicLong(0)).incrementAndGet();
    peerConnection.sendMessage(message);
  }

  public static void main(String[] args) throws InterruptedException {
    JMonitor.Session session = JMonitor.newSession("test", "test");
    TimeUnit.SECONDS.sleep(2);
    session.complete();
    logger.info(String.valueOf(session.getDurationInMillis()));
  }
}
