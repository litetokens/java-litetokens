package org.tron.common.overlay.discover.message;

import com.google.protobuf.InvalidProtocolBufferException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.tron.common.overlay.discover.Node;
import org.tron.protos.Discover;
import org.tron.protos.Discover.Endpoint;
import org.tron.protos.Discover.Neighbours;
import org.tron.protos.Discover.Neighbours.Builder;

@Slf4j
public class NeighborsMessage extends Message {

  private Discover.Neighbours neighbours;

  public NeighborsMessage(byte[] data) {
    super(Message.GET_PEERS, data);
    try {
      this.neighbours = Discover.Neighbours.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
    }
  }

  public NeighborsMessage(Node from, List<Node> neighbours) {
    super(Message.GET_PEERS, null);
    Builder builder = Neighbours.newBuilder()
        .setTimestamp(System.currentTimeMillis());

    neighbours.forEach(neighbour -> {
      Endpoint endpoint = buildEndpoint(neighbour);

      builder.addNeighbours(endpoint);
    });

    Endpoint fromEndpoint = buildEndpoint(from);

    builder.setFrom(fromEndpoint);

    this.neighbours = builder.build();

    this.data = this.neighbours.toByteArray();
  }

  public List<Node> getNodes(){
    List<Node> nodes = new ArrayList<>();
    neighbours.getNeighboursList().forEach(neighbour -> nodes.add(makeNode(neighbour)));
    return nodes;
  }

  @Override
  public byte[] getNodeId() {
    return this.neighbours.getFrom().getNodeId().toByteArray();
  }

  @Override
  public String toString() {
    return "[neighbours: " + neighbours;
  }

}
