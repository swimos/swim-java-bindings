package ai.swim.server.schema;

import ai.swim.server.SwimServerException;
import ai.swim.server.agent.AbstractAgent;
import ai.swim.server.agent.context.AgentContext;
import ai.swim.server.annotations.SwimAgent;
import ai.swim.server.annotations.SwimLane;
import ai.swim.server.annotations.SwimPlane;
import ai.swim.server.annotations.SwimRoute;
import ai.swim.server.annotations.Transient;
import ai.swim.server.lanes.value.ValueLane;
import ai.swim.server.plane.AbstractPlane;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static ai.swim.server.lanes.Lanes.valueLane;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PlaneSchemaTest {

  @Test
  void testReflectAgentSchema() throws SwimServerException {
    AgentSchema<TestAgent> agentSchema = AgentSchema.reflectSchema(TestAgent.class);
    assertEquals(
        agentSchema,
        new AgentSchema<>(
            TestAgent.class,
            "Agent",
            Map.of("testValueLane", new LaneSchema(true, LaneKind.Value,
                                                   0))));
  }

  @Test
  void testReflectPlaneSchema() throws SwimServerException {
    PlaneSchema<TestPlane> schema = PlaneSchema.reflectSchema(TestPlane.class);
    Map<Class<? extends AbstractAgent>, String> uriResolver = Map.of(TestAgent.class, "testRoute");
    assertEquals(
        schema,
        new PlaneSchema<>(
            TestPlane.class, "testPlane",
            Map.of(
                "testRoute",
                new AgentSchema<>(
                    TestAgent.class, "Agent",
                    Map.of(
                        "testValueLane",
                        new LaneSchema(true, LaneKind.Value, 0)))), uriResolver));
  }

  @SwimAgent("Agent")
  private static class TestAgent extends AbstractAgent {
    @Transient
    @SwimLane("testValueLane")
    private final ValueLane<Integer> lane = valueLane(Integer.class);

    protected TestAgent(AgentContext context) {
      super(context);
    }
  }

  @SwimPlane("testPlane")
  private static class TestPlane extends AbstractPlane {
    @SwimRoute("testRoute")
    TestAgent agent;
  }
}