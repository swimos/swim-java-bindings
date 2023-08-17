package ai.swim.server.agent.context;

import ai.swim.server.agent.AgentNode;
import ai.swim.server.lanes.Lane;
import ai.swim.server.lanes.LaneModel;
import ai.swim.server.lanes.LaneView;
import ai.swim.server.schema.LaneSchema;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import java.io.IOException;
import static ai.swim.server.schema.LaneSchema.reflectLane;

public class AgentContext {
  private final long ptr;
  private AgentNode agentNode;

  public AgentContext(long ptr) {
    this.ptr = ptr;
  }

  void setAgent(AgentNode agentNode) {
    this.agentNode = agentNode;
  }

  /**
   * Returns the {@link Lane} associated with {@code laneUri}, if it exists.
   *
   * @param laneUri to get the associated lane for.
   * @return the {@link Lane}
   * @throws IllegalArgumentException if no associated {@link Lane} is found.
   */
  public Lane laneFor(String laneUri) {
    return agentNode.getLane(laneUri);
  }

  public <L extends LaneView> void openLane(L lane, String laneUri, boolean isTransient) {
    if (agentNode.containsLane(laneUri)) {
      throw new IllegalArgumentException("Node already contains a lane with URI: " + laneUri);
    }

    Class<? extends LaneView> laneType = lane.getClass();
    int id = agentNode.nextLaneId();
    LaneSchema laneSchema = reflectLane(laneType, isTransient, id);

    try (MessageBufferPacker packer = MessagePack.newDefaultBufferPacker()) {
      laneSchema.pack(packer);
      AgentContextFunctionTable.openLane(ptr, packer.toByteArray());
    } catch (IOException e) {
      throw new RuntimeException("Failed to build lane schema", e);
    }

    LaneModel laneModel = lane.createLaneModel(agentNode.getCollector(), id);
    agentNode.addLane(laneUri, id, laneModel);
  }

}
