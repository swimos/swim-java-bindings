package ai.swim.server.agent;

import ai.swim.server.lanes.LaneModel;
import ai.swim.server.lanes.state.StateCollector;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.UUID;

/**
 * A wrapper around a user-defined {@link Agent} and the lanes that it contains.
 * <p>
 * This class receives events from the Rust runtime and dispatches them to the corresponding lanes. When an event is
 * received, its corresponding lane is looked up and the event is dispatched to it. This dispatch may result in N
 * lifecycle events being fired that generate subsequent events in lanes. Once the dispatch has completed, the {@link StateCollector}
 * has any pending events flushed out into a byte array that is returned to the Rust runtime for dispatching to peers.
 */
public class AgentModel implements Agent {
  private final Agent agent;
  private final Map<String, LaneModel> lanes;
  private final StateCollector collector;

  public AgentModel(Agent agent, Map<String, LaneModel> lanes, StateCollector collector) {
    this.agent = agent;
    this.lanes = lanes;
    this.collector = collector;
  }

  /**
   * Dispatch an event to {@code laneUri}
   *
   * @param laneUri the URI of the lane.
   * @param buffer  the event data.
   * @return an array containing encoded {@link ai.swim.server.lanes.models.response.LaneResponse}s.
   */
  public byte[] dispatch(String laneUri, ByteBuffer buffer) {
    lanes.get(laneUri).dispatch(buffer);
    return flushState();
  }

  /**
   * Dispatch a sync request to {@lcode laneUri} that was requested by a remote.
   *
   * @param laneUri the URI of the lane.
   * @param uuidMsb UUID most significant bits.
   * @param uuidLsb UUID least significant bits.
   * @return an array containing an encoded {@link ai.swim.server.lanes.models.response.LaneResponse} sync.
   */
  public byte[] sync(String laneUri, long uuidMsb, long uuidLsb) {
    lanes.get(laneUri).sync(new UUID(uuidMsb, uuidLsb));
    return flushState();
  }

  /**
   * Flush any pending state from the {@link StateCollector}.
   *
   * @return an array containing encoded {@link ai.swim.server.lanes.models.response.LaneResponse}s.
   */
  public byte[] flushState() {
    return collector.flushState();
  }

  /**
   * Initialise {@code laneUri} with the store data in {@code from}.
   *
   * @param laneUri to initialise.
   * @param from    the store initialisation data.
   */
  public void init(String laneUri, ByteBuffer from) {
    lanes.get(laneUri).init(from);
  }

  @Override
  public void didStart() {
    agent.didStart();
  }

  @Override
  public void didStop() {
    agent.didStop();
  }
}
