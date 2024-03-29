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

package ai.swim.structure.writer.value;

import ai.swim.structure.value.Attr;
import ai.swim.structure.value.Item;
import ai.swim.structure.value.PrimitiveValue;
import ai.swim.structure.value.Record;
import ai.swim.structure.value.Slot;
import ai.swim.structure.value.Value;
import ai.swim.structure.value.ValueItem;
import ai.swim.structure.writer.BodyWriter;
import ai.swim.structure.writer.HeaderWriter;
import ai.swim.structure.writer.StructuralWritable;
import ai.swim.structure.writer.StructuralWriter;
import ai.swim.structure.writer.WriterException;

public class ValueStructuralWritable implements StructuralWritable<Value> {
  @Override
  public <T> T writeInto(Value from, StructuralWriter<T> structuralWriter) {
    if (from.isPrimitive()) {
      PrimitiveValue primitiveValue = (PrimitiveValue) from;
      return primitiveValue.visitPrimitiveWritable(structuralWriter);
    } else if (from.isRecord()) {
      Record record = (Record) from;
      int attrCount = record.getAttrCount();
      HeaderWriter<T> header = structuralWriter.record(attrCount);

      for (int i = 0; i < attrCount; i++) {
        Attr attr = record.getAttr(i);
        header.writeAttr(attr.getKey().toString(), this, attr.getValue());
      }

      BodyWriter<T> bodyWriter = header.completeHeader(record.getItemCount());
      int itemCount = record.getItemCount();

      for (int i = 0; i < itemCount; i++) {
        Item item = record.getItem(i);
        if (item.isSlot()) {
          Slot slot = (Slot) item;
          bodyWriter.writeSlot(this, slot.getKey(), this, slot.getValue());
        } else {
          ValueItem valueItem = (ValueItem) item;
          bodyWriter.writeValue(this, valueItem.getValue());
        }
      }

      return bodyWriter.done();
    }

    throw new WriterException("Cannot write a value of type: " + from);
  }
}
