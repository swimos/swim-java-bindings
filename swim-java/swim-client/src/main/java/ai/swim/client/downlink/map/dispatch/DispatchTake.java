package ai.swim.client.downlink.map.dispatch;

@FunctionalInterface
public interface DispatchTake {
  void take(int n, boolean dispatch);
}
