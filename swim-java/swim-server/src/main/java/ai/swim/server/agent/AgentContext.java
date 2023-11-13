package ai.swim.server.agent;

import ai.swim.lang.ffi.AtomicDestructor;
import ai.swim.lang.ffi.NativeResource;
import ai.swim.server.agent.task.Schedule;
import ai.swim.server.agent.task.Task;
import ai.swim.server.agent.task.TaskRegistry;
import ai.swim.server.lanes.Lane;
import ai.swim.server.lanes.LaneModel;
import ai.swim.server.lanes.LaneView;
import ai.swim.server.schema.LaneSchema;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import java.io.IOException;
import java.time.Duration;
import java.util.UUID;
import static ai.swim.server.schema.LaneSchema.reflectLane;

/**
 * {@link AbstractAgent}-scoped context for performing various actions on an
 * agent.
 */
public class AgentContext implements NativeResource {
  private final long ptr;
  @SuppressWarnings({ "FieldCanBeLocal", "unused" })
  private final AtomicDestructor destructor;
  private final String agentName;
  private AgentNode agentNode;

  public AgentContext(long ptr, String agentName) {
    this.ptr = ptr;
    this.destructor = new AtomicDestructor(this, () -> AgentContextFunctionTable.dropHandle(ptr));
    this.agentName = agentName;
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
    assertAgentStarted();
    return agentNode.getLane(laneUri);
  }

  /**
   * Attempts to open a new lane on this agent, with a URI of {@code laneUri}.
   *
   * @param lane        to open.
   * @param laneUri     the URI of the lane.
   * @param isTransient whether the lane is transient.
   * @param <L>         the type of the lane.
   * @throws IllegalArgumentException if this agent already contains a lane of URI
   *                                  {@code laneUri}.
   */
  public <L extends LaneView> void openLane(L lane, String laneUri, boolean isTransient) {
    assertAgentStarted();

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
      // This is intentionally a RuntimeException so that it is caught by the runtime
      // and shutdown as it indicates an
      // encoding bug.
      throw new RuntimeException("Bug: Failed to build lane schema", e);
    }

    LaneModel laneModel = lane.initLaneModel(agentNode.getCollector(), id);
    agentNode.addLane(laneUri, id, laneModel);
  }

  public String getAgentName() {
    return agentName;
  }

  public Task suspend(Duration resumeAfter, Runnable runnable) {
    assertAgentStarted();

    TaskRegistry taskRegistry = agentNode.getTaskRegistry();
    Task task = taskRegistry.registerTask(this, new Schedule(1), runnable);
    UUID id = task.getId();

    AgentContextFunctionTable.suspendTask(
        ptr,
        resumeAfter.getSeconds(),
        resumeAfter.getNano(),
        id.getMostSignificantBits(),
        id.getLeastSignificantBits());

    return task;
  }

  public Task scheduleTaskIndefinitely(Duration interval, Runnable runnable) {
    assertAgentStarted();

    TaskRegistry taskRegistry = agentNode.getTaskRegistry();
    Task task = taskRegistry.registerTask(this, new Schedule(-1), runnable);
    UUID id = task.getId();

    AgentContextFunctionTable.scheduleTaskIndefinitely(
        ptr,
        interval.getSeconds(),
        interval.getNano(),
        id.getMostSignificantBits(),
        id.getLeastSignificantBits());

    return task;
  }

  public Task repeatTask(int runCount, Duration interval, Runnable runnable) {
    assertAgentStarted();

    if (runCount < 1) {
      throw new IllegalArgumentException(String.format("Run count (%s) < 1", runCount));
    }

    TaskRegistry taskRegistry = agentNode.getTaskRegistry();
    Task task = taskRegistry.registerTask(this, new Schedule(runCount), runnable);
    UUID id = task.getId();

    AgentContextFunctionTable.repeatTask(
        ptr,
        runCount,
        interval.getSeconds(),
        interval.getNano(),
        id.getMostSignificantBits(),
        id.getLeastSignificantBits());

    return task;
  }

  public void cancelTask(Task task) {
    assertAgentStarted();

    TaskRegistry taskRegistry = agentNode.getTaskRegistry();
    taskRegistry.cancelTask(task);

    UUID id = task.getId();
    AgentContextFunctionTable.cancelTask(ptr, id.getMostSignificantBits(), id.getLeastSignificantBits());
  }

  private void assertAgentStarted() {
    if (!agentNode.isRunning()) {
      throw new IllegalStateException("Attempted to use agent context before the agent had started");
    }
  }
}
