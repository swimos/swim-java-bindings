package ai.swim.server.agent;

import ai.swim.server.agent.context.AgentContext;
import ai.swim.server.lanes.state.StateCollector;
import java.nio.ByteBuffer;

/**
 * A wrapper around a user-defined {@link AbstractAgent} and the lanes that it contains.
 * <p>
 * This class receives events from the Rust runtime and dispatches them to the corresponding lanes. When an event is
 * received, its corresponding lane is looked up and the event is dispatched to it. This dispatch may result in N
 * lifecycle events being fired that generate subsequent events in lanes. Once the dispatch has completed, the {@link StateCollector}
 * has any pending events flushed out into a byte array that is returned to the Rust runtime for dispatching to peers.
 * <p>
 * This class is not thread safe and is designed to be invoked by the Rust runtime by one thread at a given time. Once
 * the JNI call has been made, the {@link AgentContext} may make calls back to the Rust runtime before returning the
 * state of this agent's lanes.
 */
// Many of the methods on this class may show as being unused but they are invoked by the Rust runtime.
@SuppressWarnings("unused")
public class AgentView {
  /**
   * The user's definition of the agent.
   */
  private final AbstractAgent agent;

  /**
   * Node binding for this agent.
   * <p>
   * Contains the agent's lanes and {@link StateCollector}.
   */
  private final AgentNode node;

  public AgentView(AbstractAgent agent, AgentNode node) {
    this.agent = agent;
    this.node = node;
  }

  /**
   * Dispatch an event to {@code laneIdx}
   *
   * @param laneIdx the URI of the lane.
   * @param buffer  the event data.
   */
  public byte[] dispatch(int laneIdx, ByteBuffer buffer) {
    return node.dispatch(laneIdx, buffer);
  }

  /**
   * Dispatch a sync request to {@code laneIdx} that was requested by a remote.
   *
   * @param laneIdx the URI of the lane.
   * @param uuidMsb UUID most significant bits.
   * @param uuidLsb UUID least significant bits.
   */
  public byte[] sync(int laneIdx, long uuidMsb, long uuidLsb) {
    return node.sync(laneIdx, uuidMsb, uuidLsb);
  }

  /**
   * Initialise {@code laneIdx} with the store data in {@code from}.
   *
   * @param laneIdx to initialise.
   * @param from    the store initialisation data.
   */
  public void init(int laneIdx, ByteBuffer from) {
    node.init(laneIdx, from);
  }

  /**
   * Invoked when the agent is started.
   */
  public void didStart() {
    this.agent.didStart();
  }

  /**
   * Invoked when the agent has stopped.
   */
  public void didStop() {
    this.agent.didStop();
  }

  /**
   * Flush any pending state from the {@link StateCollector}.
   *
   * @return an array containing encoded {@link ai.swim.server.lanes.models.response.LaneResponse}s.
   */
  public byte[] flushState() {
    return node.flushState();
  }

  public AgentNode getNode() {
    return node;
  }
}
