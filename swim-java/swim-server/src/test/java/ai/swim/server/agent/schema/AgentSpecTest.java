package ai.swim.server.agent.schema;

import ai.swim.server.agent.AbstractAgent;
import ai.swim.server.annotations.SwimAgent;
import ai.swim.server.annotations.SwimLane;
import ai.swim.server.annotations.Transient;
import ai.swim.server.lanes.value.ValueLane;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AgentSpecTest {

  @SwimAgent("Agent")
  private static class TestAgent extends AbstractAgent {
    @Transient
    @SwimLane("testValueLane")
    private final ValueLane<Integer> lane = valueLane(Integer.class);
  }

  @Test
  void testReflectSchema() throws NoSuchMethodException {
    TestAgent agent = new TestAgent();
    AgentSpec agentSpec = AgentSpec.reflectSchema(agent);
    assertEquals(agentSpec, new AgentSpec("Agent",
                                          TestAgent.class.getDeclaredConstructor(),
                                          Map.of("testValueLane", new LaneSpec(true, LaneKind.Value))));
  }

}