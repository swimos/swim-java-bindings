package ai.swim.server.lanes.lifecycle;

@FunctionalInterface
public interface OnUpdate<K,V> {
  void onUpdate(K key, V oldValue, V newValue);
}
