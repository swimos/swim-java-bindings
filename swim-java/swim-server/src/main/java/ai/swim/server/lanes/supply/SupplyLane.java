package ai.swim.server.lanes.supply;

import ai.swim.server.lanes.Lane;
import ai.swim.structure.Form;

/**
 * Swim Supply Lane interface.
 *
 * @param <T> the type this lane produces.
 */
public interface SupplyLane<T> extends Lane {
  /**
   * Returns the {@link Form} associated with the target type.
   */
  Form<T> valueForm();

  /**
   * Pushes a value.
   */
  void push(T value);
}
