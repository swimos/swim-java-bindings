package ai.swim.client.lifecycle;

import java.util.Map;

@FunctionalInterface
public interface OnClear<K, V> {
  void onClear(Map<K, V> map);
}