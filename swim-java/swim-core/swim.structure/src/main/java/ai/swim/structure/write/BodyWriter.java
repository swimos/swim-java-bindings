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

package ai.swim.structure.write;

import ai.swim.structure.write.proxy.WriterProxy;
import ai.swim.structure.write.std.ScalarWriters;

import java.util.Objects;

public interface BodyWriter<T> {
  <K, V> BodyWriter<T> writeSlot(Writable<K> keyWriter, K key, Writable<V> valueWriter, V value);

  default <K, V> BodyWriter<T> writeSlot(K key, V value) {
    Writable<K> keyWriter = WriterProxy.getProxy().lookupObject(key);
    Writable<V> valueWriter = WriterProxy.getProxy().lookupObject(value);
    return writeSlot(keyWriter, key, valueWriter, value);
  }

  default <K, V> BodyWriter<T> writeSlot(Writable<K> keyWriter, K key, V value) {
    Writable<V> valueWriter = WriterProxy.getProxy().lookupObject(value);
    return writeSlot(keyWriter, key, valueWriter, value);
  }

  default <K, V> BodyWriter<T> writeSlot(K key, Writable<V> valueWriter, V value) {
    Writable<K> keyWriter = WriterProxy.getProxy().lookupObject(key);
    return writeSlot(keyWriter, key, valueWriter, value);
  }

  default <V> BodyWriter<T> writeSlot(String key, Writable<V> valueWriter, V value) {
    if (valueWriter == null) {
      return writeSlot(ScalarWriters.STRING, key, value);
    } else {
      return writeSlot(ScalarWriters.STRING, key, valueWriter, value);
    }
  }

  <V> BodyWriter<T> writeValue(Writable<V> writer, V value);

  default <V> BodyWriter<T> writeValue(V value) {
    Writable<V> valueWriter = WriterProxy.getProxy().lookupObject(Objects.requireNonNull(value));
    return writeValue(valueWriter, value);
  }

  T done();
}
