package ai.swim.server.agent;

import ai.swim.server.annotations.SwimAgent;
import ai.swim.server.annotations.SwimLane;
import ai.swim.server.annotations.SwimRoute;
import ai.swim.server.lanes.value.ValueLaneView;
import ai.swim.server.plane.AbstractPlane;
import static ai.swim.server.lanes.Lanes.valueLane;

class AgentTest {

  @SwimAgent
  private static class TestAgent extends AbstractAgent {
    @SwimLane
    private final ValueLaneView<Integer> numLane = valueLane(Integer.class)
        .onSet((oldValue, newValue) -> System.out.println("On set"))
        .onEvent((value) -> System.out.println("On event"));

    private TestAgent(AgentContext context) {
      super(context);
    }
  }

  private static class TestPlane extends AbstractPlane {
    @SwimRoute("test/:id")
    private TestAgent agent;
  }


}
