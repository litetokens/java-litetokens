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
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;

@Slf4j
@Value(staticConstructor = "of")
public class StatsOnhandle implements Consumer<PeerConnection> {
  public static final long ONE_MINUTE = 60 * 1000L;
  public static final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
  public static final ConcurrentHashMap<Long, AtomicLong> stats = new ConcurrentHashMap<>();
  public static final LongAdder LONG_ADDER = new LongAdder();
//  private Message message;

  @Override
  public void accept(PeerConnection peerConnection) {
    LONG_ADDER.increment();
    stats.computeIfAbsent(System.currentTimeMillis()/ONE_MINUTE, k -> new AtomicLong(0)).incrementAndGet();
  }
}
