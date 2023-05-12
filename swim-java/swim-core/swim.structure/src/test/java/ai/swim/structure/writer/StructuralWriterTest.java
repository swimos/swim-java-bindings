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

import ai.swim.structure.value.*;
import ai.swim.structure.writer.header.Header;
import ai.swim.structure.writer.std.ScalarWriters;
import ai.swim.structure.writer.value.ValueStructuralWriter;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructuralWriterTest {

  @Test
  void simple_write() {
    Prop prop = new Prop(1, 2);
    PropWriter propWriter = new PropWriter(WriteStrategy.Plain);
    Value actual = propWriter.writeInto(prop, new ValueStructuralWriter());

    Record expected = Record.of(
            List.of(Attr.ofAttr("Prop")),
            List.of(
                    Item.of(Value.of("a"), Value.of(1)),
                    Item.of(Value.of("b"), Value.of(2L))
            )
    );

    assertEquals(expected, actual);
  }

  @Test
  void simple_write_body() {
    Prop prop = new Prop(1, 2);
    PropWriter propWriter = new PropWriter(WriteStrategy.Body);
    Value actual = propWriter.writeInto(prop, new ValueStructuralWriter());

    Record expected = Record.of(
            List.of(Attr.ofAttr("Prop", Record.of(Collections.emptyList(), List.of(Item.of(Text.of("a"), Value.of(1)))))),
            List.of(Item.of(Value.of(2L)))
    );

    assertEquals(expected, actual);
  }

  @Test
  void simple_write_header_body() {
    Prop prop = new Prop(1, 2);
    PropWriter propWriter = new PropWriter(WriteStrategy.HeaderBody);
    Value actual = propWriter.writeInto(prop, new ValueStructuralWriter());

    Record expected = Record.of(
            List.of(Attr.ofAttr("Prop", Value.of(1))),
            List.of(Item.of(Text.of("b"), Value.of(2L)))
    );

    assertEquals(expected, actual);
  }

  @Test
  void manualGenericWriter() {
    GenericClass<Integer> genericClass = new GenericClass<>(13, "value");
    GenericClassWriter<Integer> genericClassWriter = new GenericClassWriter<>(ScalarWriters.INTEGER);
    Value actual = genericClassWriter.writeInto(genericClass, new ValueStructuralWriter());

    assertTrue(actual.isRecord());
    Record expected = Value.of(
            List.of(Value.ofAttr("GenericClass")),
            List.of(
                    Value.ofItem(Value.of("gen"), Value.of(13)),
                    Value.ofItem(Value.of("key"), Value.of("value"))
            )
    );

    assertEquals(expected, actual);
  }

  enum WriteStrategy {
    Plain,
    Body, // b is the body
    HeaderBody // b is the header body
  }

  static class Prop {
    public int a;
    public long b;

    public Prop(int a, long b) {
      this.a = a;
      this.b = b;
    }
  }

  static class PropWriter implements Writable<Prop> {
    private final WriteStrategy strategy;

    public PropWriter(WriteStrategy strategy) {
      this.strategy = strategy;
    }

    @Override
    public <T> T writeInto(Prop from, StructuralWriter<T> structuralWriter) {
      switch (strategy) {
        case Plain:
          return structuralWriter
                  .record(1)
                  .writeExtantAttr("Prop")
                  .completeHeader(2)
                  .writeSlot(ScalarWriters.STRING, "a", ScalarWriters.INTEGER, from.a)
                  .writeSlot(ScalarWriters.STRING, "b", ScalarWriters.LONG, from.b)
                  .done();
        case Body:
          return structuralWriter
                  .record(2)
                  .writeAttr("Prop", Header.NoSlots.prepend("a", ScalarWriters.INTEGER, from.a).simple())
                  .delegate(ScalarWriters.LONG, from.b);
        case HeaderBody:
          return structuralWriter
                  .record(1)
                  .writeAttr("Prop", ScalarWriters.INTEGER, from.a)
                  .completeHeader(1)
                  .writeSlot(ScalarWriters.STRING, "b", ScalarWriters.LONG, from.b)
                  .done();
        default:
          throw new AssertionError(strategy);
      }
    }
  }

  private static class GenericClass<G> {
    public final G gen;
    public final String key;


    private GenericClass(G gen, String key) {
      this.gen = gen;
      this.key = key;
    }
  }

  private static class GenericClassWriter<G> implements Writable<GenericClass<G>> {
    private final Writable<G> genWriter;

    private GenericClassWriter(Writable<G> genWriter) {
      this.genWriter = genWriter;
    }

    @Override
    public <T> T writeInto(GenericClass<G> from, StructuralWriter<T> structuralWriter) {
      return structuralWriter
              .record(1)
              .writeExtantAttr("GenericClass")
              .completeHeader(2)
              .writeSlot(ScalarWriters.STRING, "gen", genWriter, from.gen)
              .writeSlot(ScalarWriters.STRING, "key", ScalarWriters.STRING, from.key)
              .done();
    }
  }

}