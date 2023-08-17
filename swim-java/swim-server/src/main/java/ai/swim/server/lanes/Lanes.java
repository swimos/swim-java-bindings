package ai.swim.server.lanes;

import ai.swim.server.lanes.value.ValueLane;
import ai.swim.server.lanes.value.ValueLaneView;
import ai.swim.structure.Form;

public class Lanes {
  /**
   * Returns a new Value Lane that encodes and decodes {@code T} events.
   *
   * @param form for encoding and decoding {@code T}
   * @param <T>  the lane's event type.
   * @return a new Value Lane.
   */
  public static <T> ValueLane<T> valueLane(Form<T> form) {
    return new ValueLaneView<>(form);
  }

  /**
   * Returns a new Value Lane that encodes and decodes {@code T} events.
   *
   * @param clazz class of {@code T}.
   * @param <T>   the lane's event type.
   * @return a new Value Lane.
   */
  public static <T> ValueLane<T> valueLane(Class<T> clazz) {
    return valueLane(Form.forClass(clazz));
  }

}
