package ai.swim.server.lanes.lifecycle;

/**
 * Callback that is invoked when a value lane updated its state..
 *
 * @param <T> the lane's type.
 */
@FunctionalInterface
public interface OnSet<T> {
  /**
   * Performs this operation on the given arguments.
   *
   * @param oldValue the previous state.
   * @param newValue the new state.
   */
  void onSet(T oldValue, T newValue);
}
