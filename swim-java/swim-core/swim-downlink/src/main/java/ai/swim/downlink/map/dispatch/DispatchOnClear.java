package ai.swim.downlink.map.dispatch;

@FunctionalInterface
public interface DispatchOnClear {
  void onClear(boolean dispatch);
}
