package ai.swim.server.lanes.map;

import ai.swim.server.lanes.MapLookup;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MapState<K, V> implements MapLookup<K, V> {
  private final Map<K, V> state;

  public MapState() {
    state = new HashMap<>();
  }

  public MapState(Map<K, V> state) {
    this.state = state;
  }

  @Override
  public V get(K key) {
    return state.get(key);
  }

  public void clear() {
    state.clear();
  }

  public V put(K key, V value) {
    return state.put(key, value);
  }

  public V remove(K key) {
    return state.remove(key);
  }

  public Set<K> keySet() {
    return new HashSet<>(state.keySet());
  }
}