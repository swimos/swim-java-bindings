// Copyright 2015-2022 Swim Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package ai.swim.client.downlink.map;

import ai.swim.client.downlink.DownlinkException;
import ai.swim.client.downlink.TriConsumer;
import ai.swim.client.lifecycle.OnClear;
import ai.swim.client.lifecycle.OnRemove;
import ai.swim.client.lifecycle.OnSynced;
import ai.swim.client.lifecycle.OnUpdate;
import ai.swim.codec.Parser;
import ai.swim.codec.ParserError;
import ai.swim.codec.input.Input;
import ai.swim.structure.Form;
import ai.swim.structure.FormParser;
import ai.swim.structure.recognizer.Recognizer;
import ai.swim.structure.recognizer.RecognizerException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

class MapDownlinkState<K, V> {
  private static final int DEFAULT_MAP_SIZE = 16;
  private final Form<K> keyForm;
  private final Form<V> valueForm;
  private final OnRemove<K, V> onRemove;
  private HashMap<K, V> state;

  MapDownlinkState(Form<K> keyForm, Form<V> valueForm, OnRemove<K, V> onRemove) {
    this(keyForm, valueForm, onRemove, new HashMap<>());
  }

  MapDownlinkState(Form<K> keyForm, Form<V> valueForm, OnRemove<K, V> onRemove, HashMap<K, V> init) {
    this.keyForm = keyForm;
    this.valueForm = valueForm;
    this.onRemove = onRemove;
    this.state = init;
  }

  TriConsumer<ByteBuffer, ByteBuffer, Boolean> wrapOnUpdate(OnUpdate<K, V> onUpdate) {
    return (keyBuffer, valueBuffer, dispatch) -> {
      K key;
      V value;
      try {
        key = tryParseKey(keyBuffer);
        value = tryParseValue(valueBuffer);
      } catch (RuntimeException e) {
        throw new DownlinkException("Invalid frame body", e);
      }

      V oldValue = state.put(key, value);

      if (dispatch && onUpdate != null) {
        try {
          onUpdate.onUpdate(key, Collections.unmodifiableMap(state), oldValue, value);
        } catch (Throwable e) {
          throw new DownlinkException(e);
        }
      }
    };
  }

  Routine wrapOnSynced(OnSynced<Map<K, V>> onSynced) {
    if (onSynced != null) {
      return () -> {
        try {
          onSynced.onSynced(Collections.unmodifiableMap(state));
        } catch (Throwable e) {
          throw new DownlinkException(e);
        }
      };
    } else {
      return null;
    }
  }

  BiConsumer<ByteBuffer, Boolean> wrapOnRemove(OnRemove<K, V> onRemove) {
    return (keyBuffer, dispatch) -> {
      K key;
      try {
        key = tryParseKey(keyBuffer);
      } catch (RuntimeException e) {
        throw new DownlinkException("Invalid frame body", e);
      }

      V value = state.remove(key);

      if (dispatch && onRemove != null) {
        try {
          onRemove.onRemove(key, Collections.unmodifiableMap(state), value);
        } catch (Throwable e) {
          throw new DownlinkException(e);
        }
      }
    };
  }

  /**
   * Wraps the provided on clear interface so that it is invoked with a read-only view of the map's current state.
   */
  Consumer<Boolean> wrapOnClear(OnClear<K, V> onClear) {
    return (dispatch) -> {
      if (dispatch && onClear != null) {
        try {
          onClear.onClear(Collections.unmodifiableMap(state));
        } catch (Throwable e) {
          throw new DownlinkException(e);
        }
      }
      state.clear();
    };
  }

  /**
   * Dispatches a take operation on the map; this is more efficient than repeated FFI calls for each remove operation.
   */
  BiConsumer<Integer, Boolean> take() {
    return (n, dispatch) -> {
      Iterator<Map.Entry<K, V>> entries = state.entrySet().iterator();
      HashMap<K, V> newState = new HashMap<>(Math.min(state.size() - n, DEFAULT_MAP_SIZE));

      int idx = 0;

      while (entries.hasNext() && idx < n) {
        Map.Entry<K, V> entry = entries.next();

        idx += 1;
        K key = entry.getKey();
        V value = entry.getValue();
        newState.put(key, value);
      }

      if (dispatch) {
        while (entries.hasNext()) {
          Map.Entry<K, V> entry = entries.next();

          K key = entry.getKey();
          V value = entry.getValue();

          onRemove.onRemove(key, newState, value);
        }
      }

      state = newState;
    };
  }

  /**
   * Dispatches a drop operation on the map; this is more efficient than repeated FFI calls for each remove operation.
   */
  BiConsumer<Integer, Boolean> drop() {
    return (n, dispatch) -> {
      Iterator<Map.Entry<K, V>> entries = state.entrySet().iterator();
      HashMap<K, V> newState = new HashMap<>(Math.min(state.size() - n, DEFAULT_MAP_SIZE));
      int idx = 0;

      while (entries.hasNext() && idx < n) {
        idx += 1;

        if (dispatch) {
          Map.Entry<K, V> entry = entries.next();

          K key = entry.getKey();
          V value = entry.getValue();

          onRemove.onRemove(key, newState, value);
        }
      }

      while (entries.hasNext()) {
        Map.Entry<K, V> entry = entries.next();

        K key = entry.getKey();
        V value = entry.getValue();
        newState.put(key, value);
      }

      state = newState;
    };
  }

  private K tryParseKey(ByteBuffer buffer) {
    return parse(keyForm.reset(), buffer);
  }

  private V tryParseValue(ByteBuffer buffer) {
    return parse(valueForm.reset(), buffer);
  }

  private <F> F parse(Recognizer<F> recognizer, ByteBuffer buffer) {
    Parser<F> parser = new FormParser<>(recognizer);
    parser = parser.feed(Input.byteBuffer(buffer));
    if (parser.isDone()) {
      return parser.bind();
    } else if (parser.isError()) {
      ParserError<F> error = (ParserError<F>) parser;
      throw new RecognizerException(String.format("%s at: %s", error.cause(), error.location()));
    } else {
      throw new RecognizerException("Unconsumed input");
    }
  }

}
