package ai.swim.server.agent;

import ai.swim.lang.ffi.AtomicDestructor;
import ai.swim.lang.ffi.NativeResource;
import ai.swim.server.lanes.Lane;
import ai.swim.server.lanes.LaneModel;
import ai.swim.server.lanes.LaneView;
import ai.swim.server.schema.LaneSchema;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import java.io.IOException;
import static ai.swim.server.schema.LaneSchema.reflectLane;

/**
 * {@link AbstractAgent}-scoped context for performing various actions on an agent.
 */
public class AgentContext implements NativeResource {
  private final long ptr;
  @SuppressWarnings({"FieldCanBeLocal", "unused"})
  private final AtomicDestructor destructor;
  private AgentNode agentNode;

  public AgentContext(long ptr) {
    this.ptr = ptr;
    this.destructor = new AtomicDestructor(this, () -> AgentContextFunctionTable.dropHandle(ptr));
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

  /**
   * Attempts to open a new lane on this agent, with a URI of {@code laneUri}.
   *
   * @param lane        to open.
   * @param laneUri     the URI of the lane.
   * @param isTransient whether the lane is transient.
   * @param <L>         the type of the lane.
   * @throws IllegalArgumentException if this agent already contains a lane of URI {@code laneUri}.
   */
  public <L extends LaneView> void openLane(L lane, String laneUri, boolean isTransient) {
    if (agentNode.containsLane(laneUri)) {
      throw new IllegalArgumentException("Node already contains a lane with URI: " + laneUri);
    }

    Class<? extends LaneView> laneType = lane.getClass();
    int id = agentNode.nextLaneId();
    LaneSchema laneSchema = reflectLane(laneType, isTransient, id);

    try (MessageBufferPacker packer = MessagePack.newDefaultBufferPacker()) {
      laneSchema.pack(packer);
      AgentContextFunctionTable.openLane(ptr, laneUri, packer.toByteArray());
    } catch (IOException e) {
      // This is intentionally a RuntimeException so that it is caught by the runtime and shutdown as it indicates an
      // encoding bug.
      throw new RuntimeException("Bug: Failed to build lane schema", e);
    }

    LaneModel laneModel = lane.initLaneModel(agentNode.getCollector(), id);
    agentNode.addLane(laneUri, id, laneModel);
  }

}
