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

public class OperationDispatcher<V, K> implements MapOperationVisitor<K, V> {
  private final MapLaneState<K, V> state;
  private final MapLaneView<K, V> view;

  public OperationDispatcher(MapLaneState<K, V> state, MapLaneView<K, V> view) {
    this.state = state;
    this.view = view;
  }

  @Override
  public void visitUpdate(K key, V value) {
    V oldValue = state.update(key, value);
    view.onUpdate(key, oldValue, value);
  }

  @Override
  public void visitRemove(K key) {
    V oldValue = state.remove(key);
    view.onRemove(key, oldValue);
  }

  @Override
  public void visitClear() {
    state.clear();
    view.onClear();
  }
}
