package ai.swim.server.agent.task;

import ai.swim.server.SwimServerException;
import ai.swim.server.agent.AbstractAgent;
import ai.swim.server.agent.AgentContext;
import ai.swim.server.agent.AgentFixture;
import ai.swim.server.agent.TaggedLaneRequest;
import ai.swim.server.agent.TaggedLaneResponse;
import ai.swim.server.annotations.SwimAgent;
import ai.swim.server.annotations.SwimLane;
import ai.swim.server.annotations.Transient;
import ai.swim.server.lanes.Lanes;
import ai.swim.server.lanes.models.request.LaneRequest;
import ai.swim.server.lanes.models.response.LaneResponse;
import ai.swim.server.lanes.value.ValueLane;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import static ai.swim.server.agent.NativeTest.runAgent;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class TaskTest {

  @SwimAgent("test")
  private static class RepeatingTaskAgent extends AbstractAgent {
    @SwimLane
    @Transient
    private final ValueLane<Integer> runCount = Lanes.valueLane(Integer.class);

    protected RepeatingTaskAgent(AgentContext context) {
      super(context);
    }

    @Override
    public void didStart() {
      getContext().repeatTask(5, Duration.ofNanos(100), () -> {
        runCount.set(runCount.get() + 1);
      });
    }
  }

  @Test
  void singleRepeatingTask() throws SwimServerException, IOException, NoSuchMethodException {
    runAgent(RepeatingTaskAgent.class, List.of(TaggedLaneRequest.value("runCount", LaneRequest.command(0))), List.of(
        TaggedLaneResponse.value("runCount", LaneResponse.event(0)),
        TaggedLaneResponse.value("runCount", LaneResponse.event(1)),
        TaggedLaneResponse.value("runCount", LaneResponse.event(2)),
        TaggedLaneResponse.value("runCount", LaneResponse.event(3)),
        TaggedLaneResponse.value("runCount", LaneResponse.event(4)),
        TaggedLaneResponse.value("runCount", LaneResponse.event(5))), AgentFixture::writeIntString, true);
  }

  @SwimAgent("test")
  private static class SuspendingTaskAgent extends AbstractAgent {
    @SwimLane
    @Transient
    private final ValueLane<Integer> runCount = Lanes.valueLane(Integer.class);

    protected SuspendingTaskAgent(AgentContext context) {
      super(context);
    }

    @Override
    public void didStart() {
      getContext().suspend(Duration.ofNanos(100), () -> {
        runCount.set(runCount.get() + 1);
      });
    }
  }

  @Test
  void singleSuspend() throws SwimServerException, IOException, NoSuchMethodException {
    runAgent(SuspendingTaskAgent.class, List.of(TaggedLaneRequest.value("runCount", LaneRequest.command(0))), List.of(
        TaggedLaneResponse.value("runCount", LaneResponse.event(0)),
        TaggedLaneResponse.value("runCount", LaneResponse.event(1))), AgentFixture::writeIntString, true);
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

  @Test
  void indefiniteTask() throws SwimServerException, IOException, NoSuchMethodException {
    runAgent(IndefiniteTaskAgent.class, List.of(TaggedLaneRequest.value("runCount", LaneRequest.command(0))), List.of(
        TaggedLaneResponse.value("runCount", LaneResponse.event(0)),
        TaggedLaneResponse.value("runCount", LaneResponse.event(1)),
        TaggedLaneResponse.value("runCount", LaneResponse.event(2)),
        TaggedLaneResponse.value("runCount", LaneResponse.event(3)),
        TaggedLaneResponse.value("runCount", LaneResponse.event(4))), AgentFixture::writeIntString, true);
  }

  @SwimAgent("test")
  private static class MultipleTasksAgent extends AbstractAgent {
    @SwimLane
    @Transient
    private final ValueLane<Integer> indefiniteRunCount = Lanes.valueLane(Integer.class);

    @SwimLane
    @Transient
    private final ValueLane<Integer> suspendedRunCount = Lanes.valueLane(Integer.class);

    @SwimLane
    @Transient
    private final ValueLane<Integer> repeatingRunCount = Lanes.valueLane(Integer.class);

    private Task indefiniteTask;
    private Task suspendedTask;
    private Task repeatingTask;

    protected MultipleTasksAgent(AgentContext context) {
      super(context);
    }

    @Override
    public void didStart() {
      AgentContext context = getContext();

      indefiniteTask = context.scheduleTaskIndefinitely(Duration.ofNanos(100), () -> {
        indefiniteRunCount.set(indefiniteRunCount.get() + 1);

        if (indefiniteTask.getRunCount() == 5) {
          indefiniteTask.cancel();

          assertFalse(indefiniteTask.isScheduled());
          assertFalse(suspendedTask.isScheduled());
          assertFalse(repeatingTask.isScheduled());
        }
      });
      suspendedTask = context.suspend(Duration.ofNanos(100), () -> suspendedRunCount.set(suspendedRunCount.get() + 1));
      repeatingTask = context.repeatTask(
          5,
          Duration.ofNanos(100),
          () -> repeatingRunCount.set(repeatingRunCount.get() + 1));
    }
  }

  @Test
  void multipleTasks() throws SwimServerException, IOException, NoSuchMethodException {
    runAgent(
        MultipleTasksAgent.class,
        List.of(
            TaggedLaneRequest.value("indefiniteRunCount", LaneRequest.command(0)),
            TaggedLaneRequest.value("suspendedRunCount", LaneRequest.command(0)),
            TaggedLaneRequest.value("repeatingRunCount", LaneRequest.command(0))),
        List.of(
            TaggedLaneResponse.value("indefiniteRunCount", LaneResponse.event(0)),
            TaggedLaneResponse.value("indefiniteRunCount", LaneResponse.event(1)),
            TaggedLaneResponse.value("indefiniteRunCount", LaneResponse.event(2)),
            TaggedLaneResponse.value("indefiniteRunCount", LaneResponse.event(3)),
            TaggedLaneResponse.value("indefiniteRunCount", LaneResponse.event(4)),
            TaggedLaneResponse.value("suspendedRunCount", LaneResponse.event(0)),
            TaggedLaneResponse.value("suspendedRunCount", LaneResponse.event(1)),
            TaggedLaneResponse.value("repeatingRunCount", LaneResponse.event(0)),
            TaggedLaneResponse.value("repeatingRunCount", LaneResponse.event(1)),
            TaggedLaneResponse.value("repeatingRunCount", LaneResponse.event(2)),
            TaggedLaneResponse.value("repeatingRunCount", LaneResponse.event(3)),
            TaggedLaneResponse.value("repeatingRunCount", LaneResponse.event(4)),
            TaggedLaneResponse.value("repeatingRunCount", LaneResponse.event(5))),
        AgentFixture::writeIntString,
        true);
  }

}
