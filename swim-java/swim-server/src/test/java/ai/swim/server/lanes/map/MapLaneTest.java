package ai.swim.server.lanes.map;

import ai.swim.server.SwimServerException;
import ai.swim.server.agent.AbstractAgent;
import ai.swim.server.agent.AgentContext;
import ai.swim.server.agent.TaggedLaneRequest;
import ai.swim.server.agent.TestLaneServer;
import ai.swim.server.annotations.SwimAgent;
import ai.swim.server.annotations.SwimLane;
import ai.swim.server.annotations.SwimPlane;
import ai.swim.server.annotations.SwimRoute;
import ai.swim.server.annotations.Transient;
import ai.swim.server.lanes.models.request.LaneRequest;
import ai.swim.server.plane.AbstractPlane;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.List;
import static ai.swim.server.lanes.Lanes.mapLane;

public class MapLaneTest {

  @SwimAgent("agentName")
  private static class TestAgent extends AbstractAgent {
    @Transient
    @SwimLane
    private final MapLane<String, Integer> mapLane = mapLane(String.class, Integer.class).onClear(() -> {
      System.out.println("On clear");
    }).onRemove(((key, value) -> {
      System.out.printf("On remote: %s -> %s%n", key, value);
    })).onUpdate(((key, oldValue, newValue) -> {
      System.out.printf("On update: %s -> %s -> %s%n", key, oldValue, newValue);
    }));

    private TestAgent(AgentContext context) {
      super(context);
    }
  }

  @SwimPlane("test")
  private static class TestPlane extends AbstractPlane {
    @SwimRoute("agent")
    private TestAgent testAgent;
  }

  @Test
  void nativeTest() throws SwimServerException, IOException {
    TestLaneServer.build(
        TestPlane.class,
        List.of(new TaggedLaneRequest<>("mapLane", LaneRequest.command(MapOperation.clear()))),
        List.of(),
        null).runServer();
  }

}
