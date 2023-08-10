package ai.swim.server.agent;

import ai.swim.server.lanes.value.ValueLaneView;
import ai.swim.structure.Form;

/**
 * Base agent class for implementing Swim Agents.
 */
public abstract class AbstractAgent implements Agent {
  /**
   * Callback that is invoked when an agent starts.
   */
  @Override
  public void didStart() {

  }

  /**
   * Callback that is invoked when an agent stops.
   */
  @Override
  public void didStop() {

  }

  /**
   * Returns a new Value Lane that encodes and decodes {@code T} events.
   *
   * @param form for encoding and decoding {@code T}
   * @param <T>  the lane's event type.
   * @return a new Value Lane.
   */
  public <T> ValueLaneView<T> valueLane(Form<T> form) {
    return new ValueLaneView<>(form);
  }

  /**
   * Returns a new Value Lane that encodes and decodes {@code T} events.
   *
   * @param clazz class of {@code T}.
   * @param <T>   the lane's event type.
   * @return a new Value Lane.
   */
  public <T> ValueLaneView<T> valueLane(Class<T> clazz) {
    return valueLane(Form.forClass(clazz));
  }

}
