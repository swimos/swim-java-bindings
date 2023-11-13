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
  private static class RepeatingTaskAgent extends AbstractAgent {
    @SwimLane
    @Transient
    private final ValueLane<Integer> runCount = Lanes.valueLane(Integer.class);
    private Task task;

    protected RepeatingTaskAgent(AgentContext context) {
      super(context);
    }

    @Override
    public void didStart() {
      AgentContext context = getContext();
      task = context.repeatTask(5, Duration.ofNanos(100), () -> {
        runCount.set(runCount.get() + 1);
      });
    }
  }

  @SwimPlane("mock")
  private static class RepeatingTaskPlane extends AbstractPlane {
    @SwimRoute("agent")
    private RepeatingTaskAgent agent;
  }

  @Test
  void singleRepeatingTask() throws SwimServerException, IOException {
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

  @SwimAgent("test")
  private static class SuspendingTaskAgent extends AbstractAgent {
    @SwimLane
    @Transient
    private final ValueLane<Integer> runCount = Lanes.valueLane(Integer.class);
    private Task task;

    protected SuspendingTaskAgent(AgentContext context) {
      super(context);
    }

    @Override
    public void didStart() {
      AgentContext context = getContext();
      task = context.suspend(Duration.ofNanos(100), () -> {
        runCount.set(runCount.get() + 1);
      });
    }
  }

  @SwimPlane("mock")
  private static class SuspendingTaskPlane extends AbstractPlane {
    @SwimRoute("agent")
    private SuspendingTaskAgent agent;
  }

  @Test
  void singleSuspend() throws SwimServerException, IOException {
    TestLaneServer.build(
        SuspendingTaskPlane.class,
        List.of(
            TaggedLaneRequest.value("runCount", LaneRequest.command(0))),
        List.of(
            TaggedLaneResponse.value("runCount", LaneResponse.event(0)),
            TaggedLaneResponse.value("runCount", LaneResponse.event(1))),
        AgentFixture::writeIntString).run();
  }

  @SwimAgent("test")
  private static class IndefiniteTaskAgent extends AbstractAgent {
    @SwimLane
    @Transient
    private final ValueLane<Integer> runCount = Lanes.valueLane(Integer.class);
    private Task task;

    protected IndefiniteTaskAgent(AgentContext context) {
      super(context);
    }

    @Override
    public void didStart() {
      AgentContext context = getContext();
      task = context.scheduleTaskIndefinitely(Duration.ofNanos(100), () -> {
        runCount.set(runCount.get() + 1);
        if (task.getRunCount() == 5) {
          task.cancel();
        }
      });
    }
  }

  @SwimPlane("mock")
  private static class IndefiniteTaskPlane extends AbstractPlane {
    @SwimRoute("agent")
    private IndefiniteTaskAgent agent;
  }

  @Test
  void indefiniteTask() throws SwimServerException, IOException {
    TestLaneServer.build(
        IndefiniteTaskPlane.class,
        List.of(
            TaggedLaneRequest.value("runCount", LaneRequest.command(0))),
        List.of(
            TaggedLaneResponse.value("runCount", LaneResponse.event(0)),
            TaggedLaneResponse.value("runCount", LaneResponse.event(1)),
            TaggedLaneResponse.value("runCount", LaneResponse.event(2)),
            TaggedLaneResponse.value("runCount", LaneResponse.event(3)),
            TaggedLaneResponse.value("runCount", LaneResponse.event(4))),
        AgentFixture::writeIntString).run();
  }

}
