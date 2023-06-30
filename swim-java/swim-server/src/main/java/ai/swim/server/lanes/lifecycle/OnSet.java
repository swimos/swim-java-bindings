package ai.swim.server.lanes.lifecycle;

@FunctionalInterface
public interface OnSet<T> {
  void onSet(T oldValue, T newValue);
}
