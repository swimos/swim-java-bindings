package ai.swim.server.lanes.lifecycle;

@FunctionalInterface
public interface OnCue<V> {
  V onCue();
}
