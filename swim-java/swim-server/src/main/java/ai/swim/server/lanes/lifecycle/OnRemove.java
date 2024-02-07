package ai.swim.server.lanes.lifecycle;

@FunctionalInterface
public interface OnRemove<K, V> {
  void onRemove(K key, V value);
}
