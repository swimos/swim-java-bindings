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

import ai.swim.codec.data.ReadBuffer;
import ai.swim.codec.decoder.Decoder;
import ai.swim.codec.decoder.DecoderException;
import ai.swim.server.lanes.LaneModel;
import ai.swim.server.lanes.LaneView;
import ai.swim.server.lanes.map.codec.MapOperationDecoder;
import ai.swim.server.lanes.state.StateCollector;
import ai.swim.structure.Form;
import ai.swim.structure.recognizer.Recognizer;
import ai.swim.structure.recognizer.RecognizerException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class MapLaneModel<K, V> extends LaneModel {
  private final MapLaneView<K, V> view;
  private final Form<K> keyForm;
  private final Form<V> valueForm;
  private final MapLaneState<K, V> state;
  private final OperationDispatcher<V, K> operationDispatcher;
  private final Initialiser<V, K> initVisitor;

  public MapLaneModel(int laneId, MapLaneView<K, V> view, StateCollector collector) {
    this.view = view;
    this.keyForm = view.keyForm();
    this.valueForm = view.valueForm();
    this.state = new MapLaneState<>(laneId, keyForm, valueForm, collector);
    this.operationDispatcher = new OperationDispatcher<>(state, view);
    this.initVisitor = new Initialiser<>(state);
  }

  @Override
  public void dispatch(ReadBuffer buffer) {
    decodeAndDispatch(buffer, keyForm.reset(), valueForm.reset(), operationDispatcher);
  }

  @Override
  public void sync(UUID remote) {
    state.sync(remote);
  }

  @Override
  public void init(ReadBuffer buffer) {
    decodeAndDispatch(buffer, keyForm.reset(), valueForm.reset(), initVisitor);
  }

  private static <K, V> void decodeAndDispatch(ReadBuffer buffer,
      Recognizer<K> keyRecognizer,
      Recognizer<V> valueRecognizer,
      MapOperationVisitor<K, V> visitor) {
    Decoder<MapOperation<K, V>> decoder = new MapOperationDecoder<>(keyRecognizer, valueRecognizer);

    boolean dispatched = false;

    while (true) {
      try {
        decoder = decoder.decode(buffer);
      } catch (DecoderException e) {
        throw new RecognizerException(e);
      }

      if (decoder.isDone()) {
        MapOperation<K, V> op = decoder.bind();
        op.accept(visitor);

        dispatched = true;
        decoder = decoder.reset();
      } else if (dispatched) {
        break;
      } else {
        throw new RecognizerException("Buffer did not contain any valid map operations");
      }
    }
  }

  @Override
  public LaneView getLaneView() {
    return view;
  }

  public void clear() {
    state.clear();
  }

  public V update(K key, V value) {
    return state.update(key, value);
  }

  public V remove(K key) {
    return state.remove(key);
  }

  public V get(K key) {
    return state.get(key);
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
