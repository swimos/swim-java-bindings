package ai.swim.downlink.map.dispatch;

@FunctionalInterface
public interface DispatchTake {
  void take(int n, boolean dispatch);
}
