package ai.swim.server.agent.task;

import ai.swim.server.SwimServerException;
import ai.swim.server.agent.AbstractAgent;
import ai.swim.server.agent.AgentContext;
import ai.swim.server.agent.AgentFixture;
import ai.swim.server.agent.TaggedLaneRequest;
import ai.swim.server.agent.TaggedLaneResponse;
import ai.swim.server.agent.TestLaneServer;
import ai.swim.server.annotations.SwimAgent;
import ai.swim.server.annotations.SwimLane;
import ai.swim.server.annotations.SwimPlane;
import ai.swim.server.annotations.SwimRoute;
import ai.swim.server.annotations.Transient;
import ai.swim.server.lanes.Lanes;
import ai.swim.server.lanes.models.request.LaneRequest;
import ai.swim.server.lanes.models.response.LaneResponse;
import ai.swim.server.lanes.value.ValueLane;
import ai.swim.server.plane.AbstractPlane;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.time.Duration;
import java.util.List;

public class TaskTest {

  @SwimAgent("test")
  private static class TestAgent extends AbstractAgent {
    @SwimLane
    @Transient
    private final ValueLane<Integer> runCount = Lanes.valueLane(Integer.class);
    private Task repeatingTask;

    protected TestAgent(AgentContext context) {
      super(context);
    }

    @Override
    public void didStart() {
      AgentContext context = getContext();
      context.repeatTask(5, Duration.ofNanos(100), () -> {
        runCount.set(runCount.get() + 1);
      });
    }
  }

  @SwimPlane("mock")
  private static class RepeatingTaskPlane extends AbstractPlane {
    @SwimRoute("agent")
    private TestAgent agent;
  }

  @Test
  void repeatingTask() throws SwimServerException, IOException {
    TestLaneServer.build(
        RepeatingTaskPlane.class,
        List.of(
            TaggedLaneRequest.value("runCount", LaneRequest.command(0))),
        List.of(
            TaggedLaneResponse.value("runCount", LaneResponse.event(0)),
            TaggedLaneResponse.value("runCount", LaneResponse.event(1)),
            TaggedLaneResponse.value("runCount", LaneResponse.event(2)),
            TaggedLaneResponse.value("runCount", LaneResponse.event(3)),
            TaggedLaneResponse.value("runCount", LaneResponse.event(4)),
            TaggedLaneResponse.value("runCount", LaneResponse.event(5))),
        AgentFixture::writeIntString).run();
  }

}
