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

package ai.swim.structure.writer.value;

import ai.swim.structure.value.Record;
import ai.swim.structure.value.Text;
import ai.swim.structure.value.Value;
import ai.swim.structure.writer.BodyWriter;
import ai.swim.structure.writer.HeaderWriter;
import ai.swim.structure.writer.Writable;
import ai.swim.structure.writer.header.WritableHeader;

/**
 * A {@code Writable} implementation that yields a {@code Value} instance.
 */
public class ValueInterpreter implements HeaderWriter<Value>, BodyWriter<Value> {
  private final ValueStructuralWriter valueWriter;
  private final Record.Builder builder;

  /**
   * Instantiates a new {@code ValueInterpreter} which will write primitive types using {@code ValueStructuralWriter}
   * and initialises the record builder with an initial attribute capacity of {@code attrCount.}
   *
   * @param valueWriter to write primitive types using.
   * @param attrCount   initial capacity of the internal record builder.
   */
  public ValueInterpreter(ValueStructuralWriter valueWriter, int attrCount) {
    this.valueWriter = valueWriter;
    this.builder = new Record.Builder(attrCount, 0);
  }

  @Override
  public <V> HeaderWriter<Value> writeAttr(String key, Writable<V> valueWriter, V value) {
    Text textKey = new Text(key);
    Value interpretedValue = valueWriter.writeInto(value, this.valueWriter);
    builder.pushAttr(textKey, interpretedValue);

    return this;
  }

  @Override
  public HeaderWriter<Value> writeAttr(String key, WritableHeader writable) {
    Text textKey = new Text(key);
    Value value = writable.writeInto(valueWriter);
    builder.pushAttr(textKey, value);

    return this;
  }

  @Override
  public <V> Value delegate(Writable<V> valueWriter, V value) {
    Value body = valueWriter.writeInto(value, this.valueWriter);
    return builder.buildDelegate(body);
  }

  @Override
  public BodyWriter<Value> completeHeader(int numItems) {
    builder.reserveItems(numItems);
    return this;
  }

  @Override
  public HeaderWriter<Value> writeExtantAttr(String key) {
    builder.pushAttr(new Text(key), Value.extant());
    return this;
  }

  @Override
  public <K, V> BodyWriter<Value> writeSlot(Writable<K> keyWriter, K key, Writable<V> valueWriter, V value) {
    Value interpretedKey = keyWriter.writeInto(key, this.valueWriter);
    Value interpretedValue = valueWriter.writeInto(value, this.valueWriter);
    builder.pushItem(interpretedKey, interpretedValue);

    return this;
  }

  @Override
  public <V> BodyWriter<Value> writeValue(Writable<V> valueWriter, V value) {
    Value interpretedValue = valueWriter.writeInto(value, this.valueWriter);
    builder.pushItem(interpretedValue);

    return this;
  }

  @Override
  public Value done() {
    return builder.build();
  }

  @Override
  public String toString() {
    return "ValueInterpreter{" +
        "valueWriter=" + valueWriter +
        ", builder=" + builder +
        '}';
  }
}
