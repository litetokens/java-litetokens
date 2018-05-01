package org.tron.common.overlay.discover.message;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.overlay.discover.Node;
import org.tron.core.config.args.Args;
import org.tron.protos.Discover;
import org.tron.protos.Discover.Endpoint;

@Slf4j
public class PingMessage extends Message {

  private Discover.PingMessage pingMessage;

  public PingMessage(byte[] data) {
    super(Message.PING, data);
    try {
      this.pingMessage = Discover.PingMessage.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
    }
  }

  public PingMessage(Node from, Node to) {
    super(Message.PING, null);
    Endpoint fromEndpoint = buildEndpoint(from);
    Endpoint toEndpoint = buildEndpoint(to);
    this.pingMessage = Discover.PingMessage.newBuilder().setVersion(Args.getInstance().getNodeP2pVersion())
        .setFrom(fromEndpoint)
        .setTo(toEndpoint)
        .setTimestamp(System.currentTimeMillis())
        .build();
    this.data = this.pingMessage.toByteArray();
  }

  public Node getFrom (){
    Endpoint from = this.pingMessage.getFrom();
    return makeNode(from);
  }

  public Node getTo(){
    Endpoint to = this.pingMessage.getTo();
    return makeNode(to);
  }

  @Override
  public byte[] getNodeId() {
    return this.pingMessage.getFrom().getNodeId().toByteArray();
  }

  @Override
  public String toString() {
    return "[pingMessage: " + pingMessage;
  }

}
