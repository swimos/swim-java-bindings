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
import java.util.Map;
import java.util.Set;

/**
 * A strongly typed {@link Map} equivalent where all operations which modify or query the state of the implementation
 * operate on the type parameters instead of {@link Object}s.
 * <p>
 * For more detailed documentation, see the equivalent {@link Map} documentation.
 *
 * @param <K> the type of keys maintained by this map.
 * @param <V> the type of mapped values.
 */
public interface TypedMap<K, V> {

  /**
   * Returns the number of entries in this map.
   *
   * @return the number of entries in this map.
   */
  int size();

  /**
   * Reruns whether this map is empty.
   *
   * @return whether this map is empty.
   */
  default boolean isEmpty() {
    return size() == 0;
  }

  /**
   * Returns the value to which the specified key is mapped, or null if this map contains no mapping for the key.
   *
   * @param key the key whose associated value is to be returned
   * @return the value to which the specified key is mapped.
   */
  V get(K key);

  /**
   * Associates the specified value with the specified key in this map.
   *
   * @param key   key with which the specified value is to be associated
   * @param value value to be associated with the specified key
   * @return the previous value associated with {@code key}, or {@code null} if there was no mapping for {@code key}.
   */
  V put(K key, V value);

  /**
   * Removes the mapping for a key from this map if it is present.
   *
   * @param key key whose mapping is to be removed from the map.
   * @return the previous value associated with {@code key}, or {@code null} if there was no mapping for {@code key}.
   */
  V remove(K key);

  /**
   * Removes all the mappings from this map.
   */
  void clear();

  /**
   * Copies all the mappings from the specified map to this map.
   *
   * @param m mappings to be stored in this map.
   */
  void putAll(TypedMap<? extends K, ? extends V> m);

  /**
   * Returns a {@link Set} of the keys contained in this map.
   *
   * @return a {@link Set} of the keys contained in this map.
   */
  Set<K> keySet();

  /**
   * Returns an immutable {@link Set} view of the mappings contained in this map.
   *
   * @return an immutable {@link Set} view of the mappings contained in this map.
   */
  Set<Map.Entry<K, V>> entrySet();

  /**
   * Returns an immutable {@link Collection} view of the values contained in this map.
   *
   * @return an immutable {@link Collection} view of the values contained in this map.
   */
  Collection<V> values();

  /**
   * Returns {@code true} if this map contains a mapping for the specified key.
   *
   * @param key key whose presence in this map is to be tested
   * @return {@code true} if this map contains a mapping for the specified key
   */
  boolean containsKey(K key);

  /**
   * Returns {@code true} if this map maps one or more keys to the  specified value.
   *
   * @param value value whose presence in this map is to be tested
   * @return {@code true} if this map maps one or more keys to the
   */
  boolean containsValue(V value);

  /**
   * Constructs a new {@link TypedMap} from a {@link Map}.
   *
   * @param map backing map for the new {@link TypedMap}.
   * @param <K> the type of keys maintained by this map.
   * @param <V> the type of mapped values.
   * @return a new {@link TypedMap}.
   */
  static <K, V> TypedHashMap<K, V> of(Map<K, V> map) {
    return new TypedHashMap<>(map);
  }

}
