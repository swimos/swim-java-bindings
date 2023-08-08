package ai.swim.client.downlink.map.dispatch;

@FunctionalInterface
public interface DispatchOnClear {
  void onClear(boolean dispatch);
}
