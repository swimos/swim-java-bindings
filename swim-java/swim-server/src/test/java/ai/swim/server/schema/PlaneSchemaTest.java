package ai.swim.server.schema;

import ai.swim.server.agent.AbstractAgent;
import ai.swim.server.annotations.SwimAgent;
import ai.swim.server.annotations.SwimLane;
import ai.swim.server.annotations.SwimPlane;
import ai.swim.server.annotations.SwimRoute;
import ai.swim.server.annotations.Transient;
import ai.swim.server.lanes.value.ValueLane;
import ai.swim.server.plane.AbstractPlane;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PlaneSchemaTest {

  @Test
  void testReflectAgentSchema() {
    AgentSchema agentSchema = AgentSchema.reflectSchema(TestAgent.class);
    assertEquals(agentSchema, new AgentSchema("Agent", Map.of("testValueLane", new LaneSchema(true, LaneKind.Value))));
  }

  @Test
  void testReflectPlaneSchema() {
    PlaneSchema schema = PlaneSchema.reflectSchema(TestPlane.class);
    assertEquals(
        schema,
        new PlaneSchema(
            "testPlane",
            Map.of(
                "testRoute",
                new AgentSchema(
                    "Agent",
                    Map.of(
                        "testValueLane",
                        new LaneSchema(true, LaneKind.Value))))));
  }

  @SwimAgent("Agent")
  private static class TestAgent extends AbstractAgent {
    @Transient
    @SwimLane("testValueLane")
    private final ValueLane<Integer> lane = valueLane(Integer.class);
  }

  @SwimPlane("testPlane")
  private static class TestPlane extends AbstractPlane {
    @SwimRoute("testRoute")
    TestAgent agent;
  }
}