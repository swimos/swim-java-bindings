package ai.swim.server.agent.lanes.demand;

import ai.swim.server.SwimServerException;
import ai.swim.server.agent.AbstractAgent;
import ai.swim.server.agent.AgentContext;
import ai.swim.server.agent.AgentFixture;
import ai.swim.server.agent.TaggedLaneRequest;
import ai.swim.server.agent.TaggedLaneResponse;
import ai.swim.server.annotations.SwimAgent;
import ai.swim.server.annotations.SwimLane;
import ai.swim.server.annotations.Transient;
import ai.swim.server.lanes.demand.DemandLane;
import ai.swim.server.lanes.models.request.LaneRequest;
import ai.swim.server.lanes.models.response.LaneResponse;
import ai.swim.server.lanes.value.ValueLane;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import static ai.swim.server.agent.NativeTest.runAgent;
import static ai.swim.server.lanes.Lanes.demandLane;
import static ai.swim.server.lanes.Lanes.valueLane;

class DemandLaneTest {
  @SwimAgent("agentName")
  private static class TestAgent extends AbstractAgent {
    private int eventCount;
    @Transient
    @SwimLane
    private final DemandLane<Integer> demandLane = demandLane(Integer.class).onCue(() -> {
      eventCount += 1;
      return eventCount;
    });

    @Transient
    @SwimLane
    private final ValueLane<Integer> valueLane = valueLane(Integer.class).onEvent((ev) -> {
      demandLane.cue();
    });

    private TestAgent(AgentContext context) {
      super(context);
    }
  }


  @Test
  void nativeTest() throws SwimServerException, IOException, NoSuchMethodException {
    UUID remote = UUID.randomUUID();
    runAgent(
        TestAgent.class,
        List.of(
            TaggedLaneRequest.value("demandLane", LaneRequest.sync(remote)),
            TaggedLaneRequest.value("valueLane", LaneRequest.command(1))),
        List.of(
            TaggedLaneResponse.value("demandLane", LaneResponse.syncEvent(remote, 1)),
            TaggedLaneResponse.value("demandLane", LaneResponse.synced(remote)),
            TaggedLaneResponse.value("valueLane", LaneResponse.event(1)),
            TaggedLaneResponse.value("demandLane", LaneResponse.event(2))),
        AgentFixture::writeIntString, false);
  }
}