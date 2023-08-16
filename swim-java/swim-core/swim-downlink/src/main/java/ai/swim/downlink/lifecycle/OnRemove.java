package ai.swim.downlink.lifecycle;

import java.util.Map;

@FunctionalInterface
public interface OnRemove<K, V> {
  void onRemove(K key, Map<K, V> map, V removed);
}