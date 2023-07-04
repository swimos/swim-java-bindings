package ai.swim.server.agent;

import ai.swim.server.lanes.value.ValueLaneView;
import ai.swim.structure.Form;

public abstract class AbstractAgent {

  public <T> ValueLaneView<T> valueLane(Form<T> form) {
    return new ValueLaneView<>(form);
  }

  public <T> ValueLaneView<T> valueLane(Class<T> clazz) {
    return valueLane(Form.forClass(clazz));
  }

  public void didStart() {

  }

  public void didStop() {

  }
}
