package org.litetokens.core.net.node;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.litetokens.core.config.args.Args;
import org.litetokens.core.exception.TraitorPeerException;
import org.litetokens.core.net.message.TransactionMessage;
import org.litetokens.core.net.message.TransactionsMessage;
import org.litetokens.core.net.peer.PeerConnection;
import org.litetokens.protos.Protocol.Inventory.InventoryType;
import org.litetokens.protos.Protocol.ReasonCode;
import org.litetokens.protos.Protocol.Transaction;
import org.litetokens.protos.Protocol.Transaction.Contract.ContractType;

@Slf4j
@Component
public class XltHandler {

  private NodeImpl nodeImpl;

  private static int MAX_XLT_SIZE = 50_000;

  private static int MAX_SMART_CONTRACT_SUBMIT_SIZE = 100;

  private static int TIME_OUT = 10 * 60 * 1000;

  private BlockingQueue<XltEvent> smartContractQueue = new LinkedBlockingQueue(MAX_XLT_SIZE);

  private BlockingQueue<Runnable> queue = new LinkedBlockingQueue();

  private int threadNum = Args.getInstance().getValidateSignThreadNum();
  private ExecutorService xltHandlePool = new ThreadPoolExecutor(threadNum, threadNum, 0L,
      TimeUnit.MILLISECONDS, queue);

  private ScheduledExecutorService smartContractExecutor = Executors.newSingleThreadScheduledExecutor();

  public void init(NodeImpl nodeImpl) {
    this.nodeImpl = nodeImpl;
    handleSmartContract();
  }

  private void handleSmartContract() {
    smartContractExecutor.scheduleWithFixedDelay(() -> {
      try {
        while (queue.size() < MAX_SMART_CONTRACT_SUBMIT_SIZE) {
          XltEvent event = smartContractQueue.take();
          if (System.currentTimeMillis() - event.getTime() > TIME_OUT) {
            logger.warn("Drop smart contract {} from peer {}.");
            continue;
          }
          xltHandlePool.submit(() -> nodeImpl.onHandleTransactionMessage(event.getPeer(), event.getMsg()));
        }
      } catch (Exception e) {
        logger.error("Handle smart contract exception", e);
      }
    }, 1000, 20, TimeUnit.MILLISECONDS);
  }

  public void handleTransactionsMessage(PeerConnection peer, TransactionsMessage msg) {
    for (Transaction xlt : msg.getTransactions().getTransactionsList()) {
      Item item = new Item(new TransactionMessage(xlt).getMessageId(), InventoryType.XLT);
      if (!peer.getAdvObjWeRequested().containsKey(item)) {
        logger.warn("Receive xlt {} from peer {} without fetch request.",
            msg.getMessageId(), peer.getInetAddress());
        peer.setSyncFlag(false);
        peer.disconnect(ReasonCode.BAD_PROTOCOL);
        return;
      }
      peer.getAdvObjWeRequested().remove(item);
      int type = xlt.getRawData().getContract(0).getType().getNumber();
      if (type == ContractType.TriggerSmartContract_VALUE || type == ContractType.CreateSmartContract_VALUE) {
        if (!smartContractQueue.offer(new XltEvent(peer, new TransactionMessage(xlt)))) {
          logger.warn("Add smart contract failed, smartContractQueue size {} queueSize {}",
              smartContractQueue.size(), queue.size());
        }
      } else {
        xltHandlePool.submit(() -> nodeImpl.onHandleTransactionMessage(peer, new TransactionMessage(xlt)));
      }
    }
  }

  public boolean isBusy() {
    return queue.size() + smartContractQueue.size() > MAX_XLT_SIZE;
  }

  class XltEvent {
    @Getter
    private PeerConnection peer;
    @Getter
    private TransactionMessage msg;
    @Getter
    private long time;

    public XltEvent(PeerConnection peer, TransactionMessage msg) {
      this.peer = peer;
      this.msg = msg;
      this.time = System.currentTimeMillis();
    }
  }
}