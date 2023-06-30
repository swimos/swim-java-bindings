package ai.swim.server.agent;

import ai.swim.server.annotations.SwimAgent;
import ai.swim.server.annotations.SwimLane;
import ai.swim.server.lanes.value.ValueLaneView;

class AgentTest {

  @SwimAgent
  private static class TestAgent extends AbstractAgent {
    @SwimLane
    private final ValueLaneView<Integer> numLane = valueLane(Integer.class)
        .onSet((oldValue, newValue) -> System.out.println("On set"))
        .onEvent((value) -> System.out.println("On event"));
  }

}
