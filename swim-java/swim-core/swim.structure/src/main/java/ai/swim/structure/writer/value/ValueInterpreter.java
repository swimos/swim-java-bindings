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
import ai.swim.structure.writer.RecordBodyKind;
import ai.swim.structure.writer.StructuralWritable;

public class ValueInterpreter implements HeaderWriter<Value>, BodyWriter<Value> {
  private final ValueWriter valueWriter;
  private Record record;

  public ValueInterpreter(ValueWriter valueWriter, Record record) {
    this.valueWriter = valueWriter;
    this.record = record;
  }

  @Override
  public <V> BodyWriter<Value> writeAttrWith(String key, StructuralWritable<V> valueWriter, V value) {
    Text textKey = new Text(key);
    Value interpretedValue = valueWriter.writeInto(value, this.valueWriter);
    this.record.pushAttr(textKey, interpretedValue);

    return this;
  }

  @Override
  public BodyWriter<Value> completeHeader(RecordBodyKind mapLike, int numItems) {
    this.record = Value.record(0, numItems);
    return this;
  }

  @Override
  public <K, V> BodyWriter<Value> writeSlotWith(StructuralWritable<K> keyWriter, K key, StructuralWritable<V> valueWriter, V value) {
    Value interpretedKey = keyWriter.writeInto(key, this.valueWriter);
    Value interpretedValue = valueWriter.writeInto(value, this.valueWriter);
    this.record.pushItem(interpretedKey, interpretedValue);

    return this;
  }

  @Override
  public <V> BodyWriter<Value> writeValueWith(StructuralWritable<V> writer, V value) {
    return null;
  }

  @Override
  public Value done() {
    return this.record;
  }

  @Override
  public String toString() {
    return "ValueInterpreter{" +
        "valueWriter=" + valueWriter +
        ", record=" + record +
        '}';
  }
}
