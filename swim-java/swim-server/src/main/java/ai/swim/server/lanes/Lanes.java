package ai.swim.server.lanes;

import ai.swim.server.lanes.value.ValueLaneView;
import ai.swim.structure.Form;

/**
 * Class for creating new Swim Lanes.
 * <p>
 * The lanes returned by the methods within this class are returned as uninitialised and must be placed within a class
 * which extends from {@link ai.swim.server.agent.AbstractAgent}, is annotated with {@link ai.swim.server.annotations.SwimAgent}
 * and the lane itself must be annotated with {@link ai.swim.server.annotations.SwimLane} for automatic initialisation
 * on the agent.
 */
public class Lanes {
  /**
   * Returns a new Value Lane that encodes and decodes {@code T} events.
   *
   * @param form for encoding and decoding {@code T}
   * @param <T>  the lane's event type.
   * @return a new Value Lane.
   */
  public static <T> ValueLaneView<T> valueLane(Form<T> form) {
    return new ValueLaneView<>(form);
  }

  /**
   * Returns a new Value Lane that encodes and decodes {@code T} events.
   *
   * @param clazz class of {@code T}.
   * @param <T>   the lane's event type.
   * @return a new Value Lane.
   */
  public static <T> ValueLaneView<T> valueLane(Class<T> clazz) {
    return valueLane(Form.forClass(clazz));
  }

}
