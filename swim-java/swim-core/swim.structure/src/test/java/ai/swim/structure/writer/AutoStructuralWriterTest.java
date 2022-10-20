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
import ai.swim.structure.value.Item;
import ai.swim.structure.value.Value;
import ai.swim.structure.writer.proxy.WriterProxy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AutoStructuralWriterTest {
  private final WriterProxy proxy = WriterProxy.getProxy();

  @AutoForm
  public static class Empty {

  }

  @Test
  void emptyClass() {
    Writable<Empty> writable = proxy.lookup(Empty.class);
    Value value = writable.asValue(new Empty());

    assertEquals(Value.ofAttrs(List.of(Value.ofAttr("Empty"))), value);
  }

  @AutoForm
  public static class SimpleClassOne {
    public int first;

    public SimpleClassOne() {

    }

    public SimpleClassOne(int first) {
      this.first = first;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      SimpleClassOne that = (SimpleClassOne) o;
      return first == that.first;
    }

    @Override
    public int hashCode() {
      return Objects.hash(first);
    }

    @Override
    public String toString() {
      return "SimpleClassOne{" +
          "first=" + first +
          '}';
    }
  }

  @Test
  void simpleClass() {
    Writable<SimpleClassOne> writable = proxy.lookup(SimpleClassOne.class);
    Value value = writable.asValue(new SimpleClassOne(1));

    assertEquals(Value.of(List.of(Value.ofAttr("SimpleClassOne")), List.of(Item.of(Value.of("first"), Value.of(1)))), value);
  }


  @AutoForm
  public static class SimpleClassTwo {
    public int first;
    public String second;

    public SimpleClassTwo() {

    }

    public SimpleClassTwo(int first, String second) {
      this.first = first;
      this.second = second;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      SimpleClassTwo that = (SimpleClassTwo) o;
      return first == that.first && Objects.equals(second, that.second);
    }

    @Override
    public int hashCode() {
      return Objects.hash(first, second);
    }

    @Override
    public String toString() {
      return "SimpleClassTwo{" +
          "first=" + first +
          ", second='" + second + '\'' +
          '}';
    }
  }

  @Test
  void simpleClass2() {
    Writable<SimpleClassTwo> writable = proxy.lookup(SimpleClassTwo.class);
    Value value = writable.asValue(new SimpleClassTwo(1, "second"));

    assertEquals(Value.of(List.of(Value.ofAttr("SimpleClassTwo")), List.of(
        Item.of(Value.of("first"), Value.of(1)),
        Item.of(Value.of("second"), Value.of("second"))
    )), value);
  }

  @AutoForm
  public static class AttrClass {
    @AutoForm.Kind(FieldKind.Attr)
    public boolean manipulatedField;
    public int first;
    public String second;

    public AttrClass() {
    }

    public AttrClass(boolean manipulatedField, int first, String second) {
      this.manipulatedField = manipulatedField;
      this.first = first;
      this.second = second;
    }

    @Override
    public String toString() {
      return "AttrClass{" +
          "manipulatedField=" + manipulatedField +
          ", first=" + first +
          ", second='" + second + '\'' +
          '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      AttrClass attrClass = (AttrClass) o;
      return manipulatedField == attrClass.manipulatedField && first == attrClass.first && Objects.equals(second, attrClass.second);
    }

    @Override
    public int hashCode() {
      return Objects.hash(manipulatedField, first, second);
    }
  }

  @Test
  void attrField() {
    Writable<AttrClass> writable = proxy.lookup(AttrClass.class);
    Value value = writable.asValue(new AttrClass(true, 1, "second"));

    assertEquals(Value.of(
        List.of(Value.ofAttr("AttrClass"), Value.ofAttr("manipulatedField", Value.of(true))),
        List.of(
            Item.of(Value.of("first"), Value.of(1)),
            Item.of(Value.of("second"), Value.of("second"))
        )
    ), value);
  }

  @AutoForm
  public static class HeaderBodyClass {
    @AutoForm.Kind(FieldKind.HeaderBody)
    public boolean manipulatedField;
    public int first;
    public String second;

    public HeaderBodyClass() {
    }

    public HeaderBodyClass(boolean manipulatedField, int first, String second) {
      this.manipulatedField = manipulatedField;
      this.first = first;
      this.second = second;
    }

    @Override
    public String toString() {
      return "HeaderBodyClass{" +
          "manipulatedField=" + manipulatedField +
          ", first=" + first +
          ", second='" + second + '\'' +
          '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      HeaderBodyClass attrClass = (HeaderBodyClass) o;
      return manipulatedField == attrClass.manipulatedField && first == attrClass.first && Objects.equals(second, attrClass.second);
    }

    @Override
    public int hashCode() {
      return Objects.hash(manipulatedField, first, second);
    }
  }

  @Test
  void headerBodyField() {
    Writable<HeaderBodyClass> writable = proxy.lookup(HeaderBodyClass.class);
    Value value = writable.asValue(new HeaderBodyClass(true, 1, "second"));

    assertEquals(Value.of(
        List.of(Value.ofAttr("HeaderBodyClass", Value.of(true))),
        List.of(
            Item.of(Value.of("first"), Value.of(1)),
            Item.of(Value.of("second"), Value.of("second"))
        )
    ), value);
  }

  @AutoForm
  public static class HeaderClass {
    @AutoForm.Kind(FieldKind.Header)
    public boolean manipulatedField;
    public int first;
    public String second;

    public HeaderClass() {
    }

    public HeaderClass(boolean manipulatedField, int first, String second) {
      this.manipulatedField = manipulatedField;
      this.first = first;
      this.second = second;
    }

    @Override
    public String toString() {
      return "HeaderClass{" +
          "manipulatedField=" + manipulatedField +
          ", first=" + first +
          ", second='" + second + '\'' +
          '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      HeaderClass attrClass = (HeaderClass) o;
      return manipulatedField == attrClass.manipulatedField && first == attrClass.first && Objects.equals(second, attrClass.second);
    }

    @Override
    public int hashCode() {
      return Objects.hash(manipulatedField, first, second);
    }
  }

  @Test
  void headerField() {
    Writable<HeaderClass> writable = proxy.lookup(HeaderClass.class);
    Value value = writable.asValue(new HeaderClass(true, 1, "second"));

    assertEquals(Value.of(
        List.of(Value.ofAttr("HeaderClass", Value.ofItems(List.of(Item.of(Value.of("manipulatedField"), Value.of(true)))))),
        List.of(
            Item.of(Value.of("first"), Value.of(1)),
            Item.of(Value.of("second"), Value.of("second"))
        )
    ), value);
  }

  @AutoForm
  public static class HeaderClass2 {
    @AutoForm.Kind(FieldKind.Header)
    public String node;
    @AutoForm.Kind(FieldKind.Header)
    public String lane;
    public int first;
    public String second;

    public HeaderClass2() {
    }

    public HeaderClass2(String node, String lane, int first, String second) {
      this.node = node;
      this.lane = lane;
      this.first = first;
      this.second = second;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      HeaderClass2 that = (HeaderClass2) o;
      return first == that.first && Objects.equals(node, that.node) && Objects.equals(lane, that.lane) && Objects.equals(second, that.second);
    }

    @Override
    public int hashCode() {
      return Objects.hash(node, lane, first, second);
    }

    @Override
    public String toString() {
      return "HeaderClass2{" +
          "node='" + node + '\'' +
          ", lane='" + lane + '\'' +
          ", first=" + first +
          ", second='" + second + '\'' +
          '}';
    }
  }

  @Test
  void headerField2() {
    Writable<HeaderClass2> writable = proxy.lookup(HeaderClass2.class);
    Value value = writable.asValue(new HeaderClass2("node", "lane", 1, "second"));

    assertEquals(Value.of(
        List.of(Value.ofAttr("HeaderClass2", Value.ofItems(List.of(
            Item.of(Value.of("node"), Value.of("node")),
            Item.of(Value.of("lane"), Value.of("lane"))
        )))),
        List.of(
            Item.of(Value.of("first"), Value.of(1)),
            Item.of(Value.of("second"), Value.of("second"))
        )
    ), value);
  }

  @AutoForm
  public static class ComplexFields {
    @AutoForm.Kind(FieldKind.HeaderBody)
    public int count;
    @AutoForm.Kind(FieldKind.Header)
    public String node;
    @AutoForm.Kind(FieldKind.Header)
    public String lane;
    public int first;
    public String second;

    public ComplexFields() {

    }

    public ComplexFields(int count, String node, String lane, int first, String second) {
      this.count = count;
      this.node = node;
      this.lane = lane;
      this.first = first;
      this.second = second;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      ComplexFields that = (ComplexFields) o;
      return count == that.count && first == that.first && Objects.equals(node, that.node) && Objects.equals(lane, that.lane) && Objects.equals(second, that.second);
    }

    @Override
    public int hashCode() {
      return Objects.hash(count, node, lane, first, second);
    }

    @Override
    public String toString() {
      return "ComplexFields{" +
          "count=" + count +
          ", node='" + node + '\'' +
          ", lane='" + lane + '\'' +
          ", first=" + first +
          ", second='" + second + '\'' +
          '}';
    }
  }

  @Test
  void complexFields() {
    Writable<ComplexFields> writable = proxy.lookup(ComplexFields.class);
    Value value = writable.asValue(new ComplexFields(13, "node", "lane", 1, "second"));

    assertEquals(Value.of(
        List.of(Value.ofAttr("ComplexFields", Value.ofItems(List.of(
            Item.of(Value.of(13)),
            Item.of(Value.of("node"), Value.of("node")),
            Item.of(Value.of("lane"), Value.of("lane"))
        )))),
        List.of(
            Item.of(Value.of("first"), Value.of(1)),
            Item.of(Value.of("second"), Value.of("second"))
        )
    ), value);
  }

  @AutoForm
  public static class RenamedField {
    @AutoForm.Name("renamed")
    public int first;
    public String second;

    public RenamedField() {

    }

    public RenamedField(int first, String second) {
      this.first = first;
      this.second = second;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      RenamedField that = (RenamedField) o;
      return first == that.first && Objects.equals(second, that.second);
    }

    @Override
    public int hashCode() {
      return Objects.hash(first, second);
    }

    @Override
    public String toString() {
      return "RenamedField{" +
          "first=" + first +
          ", second='" + second + '\'' +
          '}';
    }
  }

  @Test
  void renamedField() {
    Writable<RenamedField> writable = proxy.lookup(RenamedField.class);
    Value value = writable.asValue(new RenamedField(1, "second"));

    assertEquals(Value.of(List.of(Value.ofAttr("RenamedField")), List.of(
        Item.of(Value.of("renamed"), Value.of(1)),
        Item.of(Value.of("second"), Value.of("second"))
    )), value);
  }

  @AutoForm("SomeOtherName")
  public static class RenamedClass {
    public int first;
    public String second;

    public RenamedClass() {

    }

    public RenamedClass(int first, String second) {
      this.first = first;
      this.second = second;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      RenamedClass that = (RenamedClass) o;
      return first == that.first && Objects.equals(second, that.second);
    }

    @Override
    public int hashCode() {
      return Objects.hash(first, second);
    }

    @Override
    public String toString() {
      return "RenamedClass{" +
          "first=" + first +
          ", second='" + second + '\'' +
          '}';
    }
  }

  @Test
  void renamedClass() {
    Writable<RenamedClass> writable = proxy.lookup(RenamedClass.class);
    Value value = writable.asValue(new RenamedClass(1, "second"));

    assertEquals(Value.of(List.of(Value.ofAttr("SomeOtherName")), List.of(
        Item.of(Value.of("first"), Value.of(1)),
        Item.of(Value.of("second"), Value.of("second"))
    )), value);
  }
}