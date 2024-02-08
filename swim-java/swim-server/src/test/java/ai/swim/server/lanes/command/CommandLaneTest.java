package ai.swim.server.lanes.command;

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
import java.util.List;
import java.util.UUID;
import static ai.swim.server.agent.NativeTest.runAgent;

class CommandLaneTest {

  @SwimAgent("test")
  private static class CommandLaneAgent extends AbstractAgent {
    @SwimLane("state")
    @Transient
    private ValueLane<Integer> added = Lanes.valueLane(Integer.class);
    @SwimLane("command")
    @Transient
    private CommandLane<Integer> commandLane = Lanes
        .commandLane(Integer.class)
        .onCommand((value -> added.set(value + 1)));

    protected CommandLaneAgent(AgentContext context) {
      super(context);
    }
  }

  @Test
  void sync() throws SwimServerException, IOException, NoSuchMethodException {
    UUID peer = UUID.randomUUID();
    runAgent(
        CommandLaneAgent.class,
        List.of(TaggedLaneRequest.value("command", LaneRequest.sync(peer))),
        List.of(TaggedLaneResponse.value("command", LaneResponse.synced(peer))),
        AgentFixture::writeIntString,
        true);
  }

  @Test
  void command() throws SwimServerException, IOException, NoSuchMethodException {
    runAgent(CommandLaneAgent.class, List.of(TaggedLaneRequest.value("command", LaneRequest.command(1))), List.of(
        TaggedLaneResponse.value("command", LaneResponse.event(1)),
        TaggedLaneResponse.value("state", LaneResponse.event(2))), AgentFixture::writeIntString, true);
  }

}