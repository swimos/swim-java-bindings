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

import ai.swim.codec.data.ByteWriter;
import ai.swim.server.agent.call.CallContext;
import ai.swim.server.agent.call.CallContextException;
import ai.swim.server.lanes.PendingMapWrites;
import ai.swim.server.lanes.WriteResult;
import ai.swim.server.lanes.state.State;
import ai.swim.server.lanes.state.StateCollector;
import ai.swim.structure.Form;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MapLaneState<K, V> implements State {
  private final Form<K> keyForm;
  private final Form<V> valueForm;
  private final StateCollector collector;
  private final PendingMapWrites<K, V> pendingWrites;
  private final int laneId;
  private final TypedHashMap<K, V> state;

  public MapLaneState(int laneId, Form<K> keyForm, Form<V> valueForm, StateCollector collector) {
    this.laneId = laneId;
    this.keyForm = keyForm;
    this.valueForm = valueForm;
    this.collector = collector;
    state = new TypedHashMap<>();
    pendingWrites = new PendingMapWrites<>();
  }

  /**
   * Clears the map.
   *
   * @throws CallContextException if not invoked from a valid call context.
   */
  public void clear() {
    CallContext.check();

    state.clear();
    pendingWrites.pushOperation(MapOperation.clear());
    collector.add(this);
  }

  /**
   * Associates the specified value with the specified key in this map.
   *
   * @return the old value associated with the key
   * @throws CallContextException if not invoked from a valid call context.
   */
  public V update(K key, V value) {
    CallContext.check();

    V oldValue = state.put(key, value);
    pendingWrites.pushOperation(MapOperation.update(key, value));
    collector.add(this);

    return oldValue;
  }

  /**
   * Removes the mapping for a key from this map if it is present.
   *
   * @return the old value associated with the key
   * @throws CallContextException if not invoked from a valid call context.
   */
  public V remove(K key) {
    CallContext.check();

    V oldValue = state.remove(key);
    pendingWrites.pushOperation(MapOperation.remove(key));
    collector.add(this);

    return oldValue;
  }

  /**
   * Gets the value associated with the key.
   *
   * @throws CallContextException if not invoked from a valid call context.
   */
  public V get(K key) {
    CallContext.check();
    return state.get(key);
  }

  @Override
  public WriteResult writeInto(ByteWriter bytes) {
    return pendingWrites.writeInto(laneId, state, bytes, keyForm, valueForm);
  }

  public void sync(UUID uuid) {
    pendingWrites.pushSync(uuid, state.keySet().iterator());
    collector.add(this);
  }

  public int size() {
    return state.size();
  }

  public boolean containsKey(K key) {
    return state.containsKey(key);
  }

  public boolean containsValue(V value) {
    return state.containsValue(value);
  }

  public void putAll(TypedMap<? extends K, ? extends V> m) {
    state.putAll(m);
  }

  public Set<K> keySet() {
    return state.keySet();
  }

  public Collection<V> values() {
    return state.values();
  }

  public Set<Map.Entry<K, V>> entrySet() {
    return state.entrySet();
  }

}