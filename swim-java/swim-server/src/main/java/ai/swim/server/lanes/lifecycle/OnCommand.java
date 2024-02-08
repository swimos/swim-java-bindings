package ai.swim.server.lanes.lifecycle;

@FunctionalInterface
public interface OnCommand<T> {
  void onCommand(T value);
}
