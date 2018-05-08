package org.tron.program.cat;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.overlay.message.Message;
import org.tron.core.net.peer.PeerConnection;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

@Slf4j
@Value(staticConstructor = "of")
public class StatsOnhandle implements Consumer<PeerConnection> {
  public static final long ONE_MINUTE = 60 * 1000L;
  private static final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
  private static final ConcurrentHashMap<Long, AtomicLong> stats = new ConcurrentHashMap<>();
//  private Message message;

  static {
      service.scheduleAtFixedRate(() -> logger.info("*****net recive tps:" + stats),
          10, 5, TimeUnit.SECONDS);
  }

  @Override
  public void accept(PeerConnection peerConnection) {
    stats.computeIfAbsent(System.currentTimeMillis()/ONE_MINUTE, AtomicLong::new).incrementAndGet();
  }
}
