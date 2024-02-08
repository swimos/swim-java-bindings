package ai.swim.server.agent.lanes.supply;

import ai.swim.server.SwimServerException;
import ai.swim.server.agent.AbstractAgent;
import ai.swim.server.agent.AgentContext;
import ai.swim.server.agent.AgentFixture;
import ai.swim.server.agent.TaggedLaneRequest;
import ai.swim.server.agent.TaggedLaneResponse;
import ai.swim.server.annotations.SwimAgent;
import ai.swim.server.annotations.SwimLane;
import ai.swim.server.annotations.SwimPlane;
import ai.swim.server.annotations.SwimRoute;
import ai.swim.server.annotations.Transient;
import ai.swim.server.lanes.models.request.LaneRequest;
import ai.swim.server.lanes.models.response.LaneResponse;
import ai.swim.server.lanes.supply.SupplyLane;
import ai.swim.server.lanes.value.ValueLane;
import ai.swim.server.plane.AbstractPlane;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import static ai.swim.server.agent.NativeTest.runAgent;
import static ai.swim.server.lanes.Lanes.supplyLane;
import static ai.swim.server.lanes.Lanes.valueLane;

class SupplyLaneTest {
  @SwimAgent("agentName")
  private static class TestAgent extends AbstractAgent {
    private int eventCount;
    @Transient
    @SwimLane
    private final SupplyLane<Integer> supplyLane = supplyLane(Integer.class);

    @Transient
    @SwimLane
    private final ValueLane<Integer> valueLane = valueLane(Integer.class).onEvent(supplyLane::push);

    private TestAgent(AgentContext context) {
      super(context);
    }
  }

  @Test
  void sync() throws SwimServerException, IOException, NoSuchMethodException {
    UUID remote = UUID.randomUUID();
    runAgent(
        TestAgent.class,
        List.of(TaggedLaneRequest.value("supplyLane", LaneRequest.sync(remote))),
        List.of(TaggedLaneResponse.value("supplyLane", LaneResponse.synced(remote))),
        AgentFixture::writeIntString, true);
  }

  @Test
  void events() throws SwimServerException, IOException, NoSuchMethodException {
    runAgent(TestAgent.class, List.of(TaggedLaneRequest.value("valueLane", LaneRequest.command(1))), List.of(
        TaggedLaneResponse.value("valueLane", LaneResponse.event(1)),
        TaggedLaneResponse.value("supplyLane", LaneResponse.event(1))), AgentFixture::writeIntString, true);
  }
}