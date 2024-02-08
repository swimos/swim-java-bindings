/*
 * Copyright 2015-2024 Swim Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.swim.server.lanes.map;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TypedHashMap<K, V> implements TypedMap<K, V>, MapLookup<K, V> {
  private final Map<K, V> state;

  public TypedHashMap() {
    state = new HashMap<>();
  }

  public TypedHashMap(Map<K, V> state) {
    this.state = state;
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
    return Collections.unmodifiableSet(state.keySet());
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
