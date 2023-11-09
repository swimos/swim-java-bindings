package ai.swim.server.lanes.lifecycle;

/**
 * Callback that is invoked when a value lane receives an event.
 *
 * @param <T> the lane's type.
 */
@FunctionalInterface
public interface OnEvent<T> {
  /**
   * Performs this operation on the given argument.
   *
   * @param value the received event.
   */
  void onEvent(T value);
}
