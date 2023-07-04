package ai.swim.server.lanes.lifecycle;

@FunctionalInterface
public interface OnEvent<T> {
  void onEvent(T value);
}
