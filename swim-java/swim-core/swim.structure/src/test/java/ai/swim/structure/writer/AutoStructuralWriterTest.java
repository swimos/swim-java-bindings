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

import ai.swim.structure.annotations.AutoForm;
import ai.swim.structure.annotations.FieldKind;
import ai.swim.structure.writer.std.ScalarWriters;
import org.junit.jupiter.api.Test;

class AutoStructuralWriterTest {

  @Test
  void simple_write() {
//    Prop prop = new Prop(1, 2);
//    PropWriter propWriter = new PropWriter(WriteStrategy.Plain);
//    Value actual = propWriter.writeInto(prop, new ValueStructuralWriter());
//
//    Record expected = Record.of(
//        List.of(Attr.ofAttr("Prop")),
//        List.of(
//            Item.of(Value.of("a"), Value.of(1)),
//            Item.of(Value.of("b"), Value.of(2L))
//        )
//    );
//
//    assertEquals(expected, actual);
  }

  static class Prop {
    public int a;
    public long b;

    public Prop(int a, long b) {
      this.a = a;
      this.b = b;
    }
  }

  @AutoForm(subTypes = {@AutoForm.Type(Event.class)})
  public abstract static class Envelope<B> {
    public String lane;
    public String node;
  }

  @AutoForm
  public static class Event<T extends Number, C extends CharSequence> extends Envelope<T> {
    @AutoForm.Kind(FieldKind.Header)
    public T value;

//    @AutoForm.Kind(FieldKind.HeaderBody)
//    public int body;

    public C c;

    public Event() {

    }

    public Event(T i) {
      this.value = i;
    }
  }
//
//  public static class EventWriter<E> implements Writable<Event<E>> {
//    @Override
//    public <T> T writeInto(Event<E> from, StructuralWriter<T> structuralWriter) {
//      int numAttrs = 1;
//
//      HeaderWriter<T> record = structuralWriter.record(numAttrs);
//
//      record.writeExtantAttr("Prop");
//
//      BodyWriter<T> bodyWriter = record.completeHeader(0);
//      if (from.lane != null) {
//        bodyWriter.writeSlot("lane", ScalarWriters.STRING);
//      }
//      if (from.node != null) {
//        bodyWriter.writeSlot("node", ScalarWriters.STRING);
//      }
//      if (from.value != null) {
//        bodyWriter.writeSlot(ScalarWriters.STRING, "value", from.value);
//      }
//
//      return bodyWriter.done();
//    }
//  }

}