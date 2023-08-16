package ai.swim.downlink.map.dispatch;

@FunctionalInterface
public interface DispatchDrop {
  void drop(int n, boolean dispatch);
}
