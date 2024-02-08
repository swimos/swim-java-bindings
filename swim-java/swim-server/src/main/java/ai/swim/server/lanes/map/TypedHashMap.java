package ai.swim.server.lanes.map;

import ai.swim.server.lanes.MapLookup;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class TypedHashMap<K, V> implements TypedMap<K, V>, MapLookup<K, V> {
  private final LinkedHashMap<K, V> state;

  public TypedHashMap() {
    state = new LinkedHashMap<>();
  }

  public TypedHashMap(Map<K, V> state) {
    this.state = new LinkedHashMap<>(state);
  }

  @Override
  public int size() {
    return state.size();
  }

  @Override
  public V get(K key) {
    return state.get(key);
  }

  @Override
  public V put(K key, V value) {
    return state.put(key, value);
  }

  @Override
  public V remove(K key) {
    return state.remove(key);
  }

  @Override
  public void clear() {
    state.clear();
  }

  @Override
  public void putAll(TypedMap<? extends K, ? extends V> m) {
    if (TypedHashMap.class.isAssignableFrom(m.getClass())) {
      TypedHashMap<? extends K, ? extends V> typedHashMap = (TypedHashMap<? extends K, ? extends V>) m;
      state.putAll(typedHashMap.state);
    } else {
      for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
        state.put(entry.getKey(), entry.getValue());
      }
    }
  }

  @Override
  public Set<K> keySet() {
    return state.keySet();
  }

  @Override
  public Set<Map.Entry<K, V>> entrySet() {
    return Collections.unmodifiableSet(state.entrySet());
  }

  @Override
  public Collection<V> values() {
    return Collections.unmodifiableCollection(state.values());
  }

  @Override
  public boolean containsKey(K key) {
    return state.containsKey(key);
  }

  @Override
  public boolean containsValue(V value) {
    return state.containsValue(value);
  }

}
