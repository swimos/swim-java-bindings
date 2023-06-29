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

package ai.swim.structure.writer;

import ai.swim.structure.writer.proxy.WriterProxy;
import ai.swim.structure.writer.std.ScalarWriters;

import java.util.Objects;

/**
 * Interface for defining how body items should be written in to {@code T}.
 *
 * @param <T> the type this writer produces.
 */
public interface BodyWriter<T> {

  /**
   * Writes a slot into the body.
   *
   * @param keyWriter   an interpreter for {@code key}.
   * @param key         of the slot.
   * @param valueWriter an interpreter for {@code value}.
   * @param value       of the slot.
   * @param <K>         the type of the key.
   * @param <V>         the type of the value.
   * @return this.
   */
  <K, V> BodyWriter<T> writeSlot(Writable<K> keyWriter, K key, Writable<V> valueWriter, V value);

  /**
   * Writes a slot into the body.
   *
   * @param key   of the slot.
   * @param value of the slot.
   * @param <K>   the type of the key.
   * @param <V>   the type of the value.
   * @return this.
   */
  default <K, V> BodyWriter<T> writeSlot(K key, V value) {
    Writable<K> keyWriter = WriterProxy.getProxy().lookupObject(key);
    Writable<V> valueWriter = WriterProxy.getProxy().lookupObject(value);
    return writeSlot(keyWriter, key, valueWriter, value);
  }

  /**
   * Writes a slot into the body.
   *
   * @param keyWriter an interpreter for {@code key}.
   * @param key       of the slot.
   * @param value     of the slot.
   * @param <K>       the type of the key.
   * @param <V>       the type of the value.
   * @return this.
   */
  default <K, V> BodyWriter<T> writeSlot(Writable<K> keyWriter, K key, V value) {
    Writable<V> valueWriter = WriterProxy.getProxy().lookupObject(value);
    return writeSlot(keyWriter, key, valueWriter, value);
  }

  /**
   * Writes a slot into the body.
   *
   * @param key         of the slot.
   * @param valueWriter an interpreter for {@code value}.
   * @param value       of the slot.
   * @param <K>         the type of the key.
   * @param <V>         the type of the value.
   * @return this.
   */
  default <K, V> BodyWriter<T> writeSlot(K key, Writable<V> valueWriter, V value) {
    Writable<K> keyWriter = WriterProxy.getProxy().lookupObject(key);
    return writeSlot(keyWriter, key, valueWriter, value);
  }

  /**
   * Writes a slot into the body.
   *
   * @param key         of the slot.
   * @param valueWriter an interpreter for {@code value}.
   * @param value       of the slot.
   * @param <V>         the type of the value.
   * @return this.
   */
  default <V> BodyWriter<T> writeSlot(String key, Writable<V> valueWriter, V value) {
    if (valueWriter == null) {
      return writeSlot(ScalarWriters.STRING, key, value);
    } else {
      return writeSlot(ScalarWriters.STRING, key, valueWriter, value);
    }
  }

  /**
   * Writes a value into the body.
   *
   * @param writer an interpreter for {@code value}.
   * @param value  of the slot.
   * @param <V>    the type of the value.
   * @return this.
   */
  <V> BodyWriter<T> writeValue(Writable<V> writer, V value);

  /**
   * Writes a value into the body.
   *
   * @param value of the slot.
   * @param <V>   the type of the value.
   * @return this.
   */
  default <V> BodyWriter<T> writeValue(V value) {
    Writable<V> valueWriter = WriterProxy.getProxy().lookupObject(Objects.requireNonNull(value));
    return writeValue(valueWriter, value);
  }

  /**
   * Finish writing the body and attempt to bind a value.
   */
  T done();
}
