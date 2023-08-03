package ai.swim.server.agent;

import ai.swim.server.lanes.LaneModel;
import ai.swim.server.lanes.state.StateCollector;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.UUID;

public class AgentModel implements Agent {
  private final Agent agent;
  private final Map<String, LaneModel> lanes;
  private final StateCollector collector;

  public AgentModel(Agent agent, Map<String, LaneModel> lanes, StateCollector collector) {
    this.agent = agent;
    this.lanes = lanes;
    this.collector = collector;
  }

  public byte[] dispatch(String laneUri, ByteBuffer buffer) {
    lanes.get(laneUri).dispatch(buffer);
    return flushState();
  }

  public byte[] sync(String laneUri, long uuidMsb, long uuidLsb) {
    lanes.get(laneUri).sync(new UUID(uuidMsb, uuidLsb));
    return flushState();
  }

  public byte[] flushState(){
    return collector.flushState();
  }

  public void init(String laneUri, ByteBuffer from) {
    System.out.println("Java init: " + laneUri);
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
