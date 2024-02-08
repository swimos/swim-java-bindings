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

package ai.swim.structure.writer;

import ai.swim.structure.writer.header.WritableHeader;
import ai.swim.structure.writer.proxy.WriterProxy;

/**
 * Interface for defining how header attributes should be written in to {@code T}.
 *
 * @param <T> the type this writer produces.
 */
public interface HeaderWriter<T> {

  /**
   * Write an extant attribute into the header.
   *
   * @param key of the attribute.
   * @return this.
   */
  HeaderWriter<T> writeExtantAttr(String key);

  /**
   * Write an attribute into the header.
   *
   * @param key         of the attribute.
   * @param valueWriter the interpreter to use.
   * @param value       of the attribute.
   * @param <V>         the type of the value.
   * @return this.
   */
  <V> HeaderWriter<T> writeAttr(String key, Writable<V> valueWriter, V value);

  /**
   * Write an attribute into the header.
   *
   * @param key      of the attribute.
   * @param writable the header to write.
   * @return this.
   */
  HeaderWriter<T> writeAttr(String key, WritableHeader writable);

  /**
   * Delegate the remainder of the record to the provided value.
   *
   * @param valueWriter the interpreter to use.
   * @param value       the body.
   * @param <V>         the type of the body.
   * @return the representation of the record.
   */
  <V> T delegate(Writable<V> valueWriter, V value);

  /**
   * Delegate the remainder of the record to the provided value.
   *
   * @param value the body.
   * @param <V>   the type of the body.
   * @return the representation of the record.
   */
  default <V> T delegate(V value) {
    return delegate(WriterProxy.getProxy().lookupObject(value), value);
  }

  /**
   * Completes the header and returns a body writer initialised to hold the provided number of items.
   *
   * @param numItems in the body.
   * @return an initialised body writer.
   */
  BodyWriter<T> completeHeader(int numItems);

}
