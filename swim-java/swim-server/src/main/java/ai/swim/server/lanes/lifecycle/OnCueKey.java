package ai.swim.server.lanes.lifecycle;

@FunctionalInterface
public interface OnCueKey<K,V> {
  V onCueKey(K key);
}
