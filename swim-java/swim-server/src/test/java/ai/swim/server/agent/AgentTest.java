package ai.swim.server.agent;

import ai.swim.server.SwimServerException;
import ai.swim.server.annotations.SwimAgent;
import ai.swim.server.annotations.SwimLane;
import ai.swim.server.annotations.SwimPlane;
import ai.swim.server.annotations.SwimRoute;
import ai.swim.server.annotations.Transient;
import ai.swim.server.lanes.models.request.LaneRequest;
import ai.swim.server.lanes.models.response.LaneResponse;
import ai.swim.server.lanes.value.ValueLane;
import ai.swim.server.lanes.value.ValueLaneView;
import ai.swim.server.plane.AbstractPlane;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.List;
import static ai.swim.server.lanes.Lanes.valueLane;

class AgentTest {

  @Test
  void agentResponses() throws SwimServerException, IOException {
    TestLaneServer
        .build(
            TestPlane.class,
            List.of(new TaggedLaneRequest<>("numLane", LaneRequest.command(2))),
            List.of(
                new TaggedLaneResponse<>("numLane", LaneResponse.event(2)),
                new TaggedLaneResponse<>("plusOne", LaneResponse.event(3)),
                new TaggedLaneResponse<>("minusOne", LaneResponse.event(1))),
            AgentFixture::writeInt)
        .run();
  }

  @Test
  void dynamicLaneOpens() throws SwimServerException, IOException {
    TestLaneServer.build(
        DynamicTestPlane.class,
        List.of(
            new TaggedLaneRequest<>("numLane", LaneRequest.command(13)),
            new TaggedLaneRequest<>("numLane", LaneRequest.command(14))),
        List.of(
            new TaggedLaneResponse<>("numLane", LaneResponse.event(13)),
            new TaggedLaneResponse<>("dynamic", LaneResponse.event(15)),
            new TaggedLaneResponse<>("numLane", LaneResponse.event(14)),
            new TaggedLaneResponse<>("dynamic", LaneResponse.event(16))),
        AgentFixture::writeInt).run();
  }

  @SwimAgent
  private static class TestAgent extends AbstractAgent {
    @Transient
    @SwimLane
    private final ValueLane<Integer> plusOne = valueLane(Integer.class);
    @Transient
    @SwimLane
    private final ValueLane<Integer> minusOne = valueLane(Integer.class);
    @Transient
    @SwimLane
    private final ValueLaneView<Integer> numLane = valueLane(Integer.class).onEvent(this::forward);

    private TestAgent(AgentContext context) {
      super(context);
    }

    private void forward(int ev) {
      plusOne.set(ev + 1);
      minusOne.set(ev - 1);
    }
  }

  @SwimPlane("mock")
  private static class TestPlane extends AbstractPlane {
    @SwimRoute("agent")
    private TestAgent agent;
  }

  @SwimAgent
  private static class DynamicTestAgent extends AbstractAgent {
    @Transient
    @SwimLane
    private final ValueLaneView<Integer> numLane = valueLane(Integer.class).onEvent(this::onEvent);

    private DynamicTestAgent(AgentContext context) {
      super(context);
    }

    private void onEvent(int ev) {
      if (ev == 13) {
        ValueLaneView<Integer> lane = valueLane(Integer.class);
        getContext().openLane(lane, "dynamic", true);
        lane.set(15);
      } else if (ev == 14) {
        @SuppressWarnings("unchecked") ValueLaneView<Integer> lane = (ValueLaneView<Integer>) getContext().laneFor(
            "dynamic");
        lane.set(16);
      }
    }
  }

  @SwimPlane("mock")
  private static class DynamicTestPlane extends AbstractPlane {
    @SwimRoute("agent")
    private DynamicTestAgent agent;
  }
}
