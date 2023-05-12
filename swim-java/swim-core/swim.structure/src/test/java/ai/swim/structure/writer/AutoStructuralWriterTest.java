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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("unchecked")
class AutoStructuralWriterTest {
  private final WriterProxy proxy = WriterProxy.getProxy();

  @Test
  void emptyClass() {
    Writable<Empty> writable = proxy.lookup(Empty.class);
    Value value = writable.asValue(new Empty());

    assertEquals(Value.ofAttrs(List.of(Value.ofAttr("Empty"))), value);
  }

  @Test
  void simpleClass() {
    Writable<SimpleClassOne> writable = proxy.lookup(SimpleClassOne.class);
    Value value = writable.asValue(new SimpleClassOne(1));

    assertEquals(Value.of(List.of(Value.ofAttr("SimpleClassOne")), List.of(Item.of(Value.of("first"), Value.of(1)))), value);
  }

  @Test
  void simpleClass2() {
    Writable<SimpleClassTwo> writable = proxy.lookup(SimpleClassTwo.class);
    Value value = writable.asValue(new SimpleClassTwo(1, "second"));

    assertEquals(Value.of(List.of(Value.ofAttr("SimpleClassTwo")), List.of(Item.of(Value.of("first"), Value.of(1)), Item.of(Value.of("second"), Value.of("second")))), value);
  }

  @Test
  void attrField() {
    Writable<AttrClass> writable = proxy.lookup(AttrClass.class);
    Value value = writable.asValue(new AttrClass(true, 1, "second"));

    assertEquals(Value.of(List.of(Value.ofAttr("AttrClass"), Value.ofAttr("manipulatedField", Value.of(true))), List.of(Item.of(Value.of("first"), Value.of(1)), Item.of(Value.of("second"), Value.of("second")))), value);
  }

  @Test
  void headerBodyField() {
    Writable<HeaderBodyClass> writable = proxy.lookup(HeaderBodyClass.class);
    Value value = writable.asValue(new HeaderBodyClass(true, 1, "second"));

    assertEquals(Value.of(List.of(Value.ofAttr("HeaderBodyClass", Value.of(true))), List.of(Item.of(Value.of("first"), Value.of(1)), Item.of(Value.of("second"), Value.of("second")))), value);
  }

  @Test
  void headerField() {
    Writable<HeaderClass> writable = proxy.lookup(HeaderClass.class);
    Value value = writable.asValue(new HeaderClass(true, 1, "second"));

    assertEquals(Value.of(List.of(Value.ofAttr("HeaderClass", Value.ofItems(List.of(Item.of(Value.of("manipulatedField"), Value.of(true)))))), List.of(Item.of(Value.of("first"), Value.of(1)), Item.of(Value.of("second"), Value.of("second")))), value);
  }

  @Test
  void headerField2() {
    Writable<HeaderClass2> writable = proxy.lookup(HeaderClass2.class);
    Value value = writable.asValue(new HeaderClass2("node", "lane", 1, "second"));

    assertEquals(Value.of(List.of(Value.ofAttr("HeaderClass2", Value.ofItems(List.of(Item.of(Value.of("node"), Value.of("node")), Item.of(Value.of("lane"), Value.of("lane")))))), List.of(Item.of(Value.of("first"), Value.of(1)), Item.of(Value.of("second"), Value.of("second")))), value);
  }

  @Test
  void complexFields() {
    Writable<ComplexFields> writable = proxy.lookup(ComplexFields.class);
    Value value = writable.asValue(new ComplexFields(13, "node", "lane", 1, "second"));

    assertEquals(Value.of(List.of(Value.ofAttr("ComplexFields", Value.ofItems(List.of(Item.of(Value.of(13)), Item.of(Value.of("node"), Value.of("node")), Item.of(Value.of("lane"), Value.of("lane")))))), List.of(Item.of(Value.of("first"), Value.of(1)), Item.of(Value.of("second"), Value.of("second")))), value);
  }

  @Test
  void renamedField() {
    Writable<RenamedField> writable = proxy.lookup(RenamedField.class);
    Value value = writable.asValue(new RenamedField(1, "second"));

    assertEquals(Value.of(List.of(Value.ofAttr("RenamedField")), List.of(Item.of(Value.of("renamed"), Value.of(1)), Item.of(Value.of("second"), Value.of("second")))), value);
  }

  @Test
  void renamedClass() {
    Writable<RenamedClass> writable = proxy.lookup(RenamedClass.class);
    Value value = writable.asValue(new RenamedClass(1, "second"));

    assertEquals(Value.of(Value.ofAttr("SomeOtherName"), Item.of(Value.of("first"), Value.of(1)), Item.of(Value.of("second"), Value.of("second"))), value);
  }

  @Test
  void nestedClasses() {
    Writable<Outer> writable = proxy.lookup(Outer.class);
    Value value = writable.asValue(new Outer(new Inner(13, "things")));

    assertEquals(Value.of(Value.ofAttr("Outer"), Item.of(Value.of("inner"), Value.of(Value.ofAttr("Inner"), Item.of(Value.of("first"), Value.of(13)), Item.of(Value.of("second"), Value.of("things"))))), value);
  }

  @Test
  void bodyReplaced() {
    Writable<BodyReplaced> writable = proxy.lookup(BodyReplaced.class);
    Value value = writable.asValue(new BodyReplaced(1, "second"));

    assertEquals(Value.of(List.of(Value.ofAttr("BodyReplaced", Value.ofItems(Item.of(Value.of("first"), Value.of(1))))), List.of(Item.of(Value.of("second")))), value);
  }

  @Test
  void nestedClassesBody() {
    Writable<OuterBody> writable = proxy.lookup(OuterBody.class);
    Value value = writable.asValue(new OuterBody("val", new InnerBody(13, "things")));

    assertEquals(Value.of(List.of(Value.ofAttr("OuterBody", Value.ofItems(Item.of(Value.of("var"), Value.of("val")))), Value.ofAttr("InnerBody")), List.of(Item.of(Value.of("first"), Value.of(13)), Item.of(Value.of("second"), Value.of("things")))), value);
  }

  @Test
  void polymorphic() {
    Value expected = Value.of(Value.ofAttr("ValueClass", Value.ofItems(Item.of(Value.of("host"), Value.of("swim.ai")), Item.of(Value.of("lane"), Value.of("lane_uri")), Item.of(Value.of("node"), Value.of("node_uri")))), Item.of(Value.of(13)));
    ValueClass valueClass = new ValueClass(13, "lane_uri", "node_uri", "swim.ai");

    assertEquals(expected, proxy.lookup(HostIFace.class).asValue(valueClass));
    assertEquals(expected, proxy.lookup(AbsLaneClass.class).asValue(valueClass));
    assertEquals(expected, proxy.lookup(AbsNodeClass.class).asValue(valueClass));
    assertEquals(expected, proxy.lookup(ValueClass.class).asValue(valueClass));
  }

  @Test
  void genericClass() {
    Writable<GenericClassSlot<?>> genericClassWritable = proxy.lookup((Class<GenericClassSlot<?>>) (Class<?>) GenericClassSlot.class);
    Value actual = genericClassWritable.asValue(new GenericClassSlot<>("node_uri", "lane_uri", 13, 15));
    assertEquals(Value.of(Value.ofAttr("GenericClassSlot"), Item.of(Value.of("node"), Value.of("node_uri")), Item.of(Value.of("lane"), Value.of("lane_uri")), Item.of(Value.of("valueA"), Value.of(13)), Item.of(Value.of("valueB"), Value.of(15))), actual);
  }

  // Tests that a writable works when the generics are wildcard types and that the generic is looked up again when it
  // doesn't match a previously visited type.
  @Test
  void genericClassMixedInputs() {
    Writable<GenericClassSlot<?>> genericClassWritable = proxy.lookup((Class<GenericClassSlot<?>>) (Class<?>) GenericClassSlot.class);
    Value actual = genericClassWritable.asValue(new GenericClassSlot<>("node_uri", "lane_uri", 13, 15));
    assertEquals(Value.of(Value.ofAttr("GenericClassSlot"), Item.of(Value.of("node"), Value.of("node_uri")), Item.of(Value.of("lane"), Value.of("lane_uri")), Item.of(Value.of("valueA"), Value.of(13)), Item.of(Value.of("valueB"), Value.of(15))), actual);

    actual = genericClassWritable.asValue(new GenericClassSlot<>("node_uri", "lane_uri", 13, List.of(1.1, 2.2, 3.3)));
    assertEquals(Value.of(Value.ofAttr("GenericClassSlot"), Item.of(Value.of("node"), Value.of("node_uri")), Item.of(Value.of("lane"), Value.of("lane_uri")), Item.of(Value.of("valueA"), Value.of(13)), Item.of(Value.of("valueB"), Value.ofItems(Item.valueItem(1.1), Item.valueItem(2.2), Item.valueItem(3.3)))), actual);

    actual = genericClassWritable.asValue(new GenericClassSlot<>("node_uri", "lane_uri", "stringy", 13L));
    assertEquals(Value.of(Value.ofAttr("GenericClassSlot"), Item.of(Value.of("node"), Value.of("node_uri")), Item.of(Value.of("lane"), Value.of("lane_uri")), Item.of(Value.of("valueA"), Value.of("stringy")), Item.of(Value.of("valueB"), Value.of(13L))), actual);
  }

  @Test
  void mapList() {
    Map firstMap = new TreeMap<>();
    firstMap.put("a", 1);
    Map secondMap = new TreeMap<>();
    secondMap.put("b", 2L);
    secondMap.put("c", 3.3);

    Writable<MapList<?, ?>> genericClassWritable = proxy.lookup((Class<MapList<?, ?>>) (Class<?>) MapList.class);
    Value actual = genericClassWritable.asValue(new MapList<>(List.of(firstMap, secondMap)));

    assertEquals(Value.of(Value.ofAttr("MapList"), Item.of(Value.of("list"), Value.ofItems(Item.of(Value.ofItems(Item.of(Value.of("a"), Value.of(1)))), Item.of(Value.ofItems(Item.of(Value.of("b"), Value.of(2L)), Item.of(Value.of("c"), Value.of(3.3))))))), actual);
  }

  @Test
  void genericArrayClass() {
    Writable<GenericArrayClass<?>> arrayClass = proxy.lookup((Class<GenericArrayClass<?>>) (Class<?>) GenericArrayClass.class);
    Value actual = arrayClass.asValue(new GenericArrayClass<>(new Number[]{1L, 1.1, 3.3, 4.4d}));

    assertEquals(Value.of(Value.ofAttr("GenericArrayClass"), Item.of(Value.of("numbers"), Value.ofItems(Item.valueItem(1L), Item.valueItem(1.1), Item.valueItem(3.3), Item.valueItem(4.4d)))), actual);
  }

  @Test
  void writeValueFieldClass() {
    Writable<ValueFieldClass> writable = proxy.lookup(ValueFieldClass.class);

    assertEquals(Value.of("ValueFieldClass", Item.of(Value.of("value"), Value.extant())), writable.asValue(new ValueFieldClass(Value.extant())));
    assertEquals(Value.of("ValueFieldClass", Item.of(Value.of("value"), Value.of(13))), writable.asValue(new ValueFieldClass(Value.of(13))));
    assertEquals(Value.of("ValueFieldClass", Item.of(Value.of("value"), Value.of(13L))), writable.asValue(new ValueFieldClass(Value.of(13L))));
    assertEquals(Value.of("ValueFieldClass", Item.of(Value.of("value"), Value.of(13f))), writable.asValue(new ValueFieldClass(Value.of(13f))));
    assertEquals(Value.of("ValueFieldClass", Item.of(Value.of("value"), Value.of(13d))), writable.asValue(new ValueFieldClass(Value.of(13d))));
    assertEquals(Value.of("ValueFieldClass", Item.of(Value.of("value"), Value.of(true))), writable.asValue(new ValueFieldClass(Value.of(true))));
    assertEquals(Value.of("ValueFieldClass", Item.of(Value.of("value"), Value.of(BigInteger.TEN))), writable.asValue(new ValueFieldClass(Value.of(BigInteger.TEN))));
    assertEquals(Value.of("ValueFieldClass", Item.of(Value.of("value"), Value.of(BigDecimal.TEN))), writable.asValue(new ValueFieldClass(Value.of(BigDecimal.TEN))));
    assertEquals(Value.of("ValueFieldClass", Item.of(Value.of("value"), Value.of(new byte[]{1, 2, 3}))), writable.asValue(new ValueFieldClass(Value.of(new byte[]{1, 2, 3}))));
    assertEquals(Value.of("ValueFieldClass", Item.of(Value.of("value"), Value.of("hello", Item.valueItem(1), Item.valueItem(2), Item.valueItem(3)))), writable.asValue(new ValueFieldClass(Value.of("hello", Item.valueItem(1), Item.valueItem(2), Item.valueItem(3)))));
  }

  @Test
  void genericHeader() {
    Writable<GenericHeader<?>> writable = proxy.lookup((Class<GenericHeader<?>>) (Class<?>) GenericHeader.class);
    Value value = writable.asValue(new GenericHeader<>("node_uri", "lane_uri", 13));

    assertEquals(Value.of(List.of(Value.ofAttr("GenericHeader", Value.ofItems(List.of(Item.of(Value.of("value"), Value.of(13)))))), List.of(Item.of(Value.of("node"), Value.of("node_uri")), Item.of(Value.of("lane"), Value.of("lane_uri")))), value);
  }

  @Test
  void genericHeaderBody() {
    Writable<GenericHeaderBody<?>> writable = proxy.lookup((Class<GenericHeaderBody<?>>) (Class<?>) GenericHeaderBody.class);
    Value value = writable.asValue(new GenericHeaderBody<>("node_uri", "lane_uri", 13));

    assertEquals(Value.of(List.of(Value.ofAttr("GenericHeaderBody", Value.of(13))), List.of(Item.of(Value.of("node"), Value.of("node_uri")), Item.of(Value.of("lane"), Value.of("lane_uri")))), value);
  }

  @Test
  void genericAttr() {
    Writable<GenericAttr<?>> writable = proxy.lookup((Class<GenericAttr<?>>) (Class<?>) GenericAttr.class);
    Value value = writable.asValue(new GenericAttr<>("node_uri", "lane_uri", 13));

    assertEquals(Value.of(List.of(Value.ofAttr("GenericAttr"), Value.ofAttr("value", Value.of(13))), List.of(Item.of(Value.of("node"), Value.of("node_uri")), Item.of(Value.of("lane"), Value.of("lane_uri")))), value);
  }

  @Test
  void genericBody() {
    Writable<GenericBody<?>> writable = proxy.lookup((Class<GenericBody<?>>) (Class<?>) GenericBody.class);
    Value value = writable.asValue(new GenericBody<>("node_uri", "lane_uri", 13));

    assertEquals(Value.of(List.of(Value.ofAttr("GenericBody", Value.ofItems(Item.of(Value.of("node"), Value.of("node_uri")), Item.of(Value.of("lane"), Value.of("lane_uri"))))), List.of(Item.valueItem(13))), value);
  }

  @Test
  void simpleEnum() {
    Writable<EnumForm> writable = proxy.lookup(EnumForm.class);

    assertEquals(
        Value.of("tagA", Item.of(Value.of("fieldA"), Value.of(1)), Item.of(Value.of("b"), Value.of(2))),
        writable.asValue(EnumForm.A)
    );
    assertEquals(
        Value.of("B", Item.of(Value.of("fieldA"), Value.of(3)), Item.of(Value.of("b"), Value.of(4))),
        writable.asValue(EnumForm.B)
    );
  }

  @Test
  void enumPolymorphism() {
    assertEquals(
        Value.of("A", Item.of(Value.of("value"), Value.of("aValue"))),
        proxy.lookup((Class<EnumInterface<String>>) (Class<?>) EnumInterface.class).asValue(ChildEnum.A)
    );
    assertEquals(
        Value.of("A", Item.of(Value.of("value"), Value.of("aValue"))),
        proxy.lookup(ChildEnum.class).asValue(ChildEnum.A)
    );
  }

  @AutoForm
  public enum EnumForm {
    @AutoForm.Tag("tagA")
    A(1, 2),
    B(3, 4);

    @AutoForm.Name("fieldA")
    private final int a;
    private final int b;

    EnumForm(int a, int b) {
      this.a = a;
      this.b = b;
    }

    public int getA() {
      return a;
    }

    public int getB() {
      return b;
    }
  }

  @AutoForm
  public enum ChildEnum implements EnumInterface<String> {
    A("aValue") {
      @Override
      public String body() {
        return A.value;
      }
    },
    B("bValue") {
      @Override
      public String body() {
        return B.value;
      }
    };

    private final String value;

    ChildEnum(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }

  @AutoForm(subTypes = {@AutoForm.Type(AbsNodeClass.class)})
  public interface HostIFace {
    String host();
  }

  @AutoForm(subTypes = @AutoForm.Type(ChildEnum.class))
  public interface EnumInterface<T> {
    T body();
  }

  @AutoForm
  public static class Empty {

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
      return "SimpleClassOne{" + "first=" + first + '}';
    }
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
      return "SimpleClassTwo{" + "first=" + first + ", second='" + second + '\'' + '}';
    }
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
      return "AttrClass{" + "manipulatedField=" + manipulatedField + ", first=" + first + ", second='" + second + '\'' + '}';
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
      return "HeaderBodyClass{" + "manipulatedField=" + manipulatedField + ", first=" + first + ", second='" + second + '\'' + '}';
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
      return "HeaderClass{" + "manipulatedField=" + manipulatedField + ", first=" + first + ", second='" + second + '\'' + '}';
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
      return "HeaderClass2{" + "node='" + node + '\'' + ", lane='" + lane + '\'' + ", first=" + first + ", second='" + second + '\'' + '}';
    }
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
      return "ComplexFields{" + "count=" + count + ", node='" + node + '\'' + ", lane='" + lane + '\'' + ", first=" + first + ", second='" + second + '\'' + '}';
    }
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
        ;
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
      return "RenamedField{" + "first=" + first + ", second='" + second + '\'' + '}';
    }
  }

  @AutoForm
  @AutoForm.Tag("SomeOtherName")
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
      return "RenamedClass{" + "first=" + first + ", second='" + second + '\'' + '}';
    }
  }

  @AutoForm
  public static class Inner {
    public int first;
    public String second;

    public Inner() {
    }

    public Inner(int first, String second) {
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
      Inner inner = (Inner) o;
      return first == inner.first && Objects.equals(second, inner.second);
    }

    @Override
    public int hashCode() {
      return Objects.hash(first, second);
    }

    @Override
    public String toString() {
      return "Inner{" + "first=" + first + ", second='" + second + '\'' + '}';
    }
  }

  @AutoForm
  public static class Outer {
    public Inner inner;

    public Outer() {

    }

    public Outer(Inner inner) {
      this.inner = inner;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Outer outer = (Outer) o;
      return Objects.equals(inner, outer.inner);
    }

    @Override
    public String toString() {
      return "Outer{" + "inner=" + inner + '}';
    }

    @Override
    public int hashCode() {
      return Objects.hash(inner);
    }
  }

  @AutoForm
  public static class BodyReplaced {
    public int first;
    @AutoForm.Kind(FieldKind.Body)
    public String second;

    public BodyReplaced() {

    }

    public BodyReplaced(int first, String second) {
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
      BodyReplaced that = (BodyReplaced) o;
      return first == that.first && Objects.equals(second, that.second);
    }

    @Override
    public int hashCode() {
      return Objects.hash(first, second);
    }

    @Override
    public String toString() {
      return "BodyReplaced{" + "first=" + first + ", second='" + second + '\'' + '}';
    }
  }

  @AutoForm
  public static class InnerBody {
    public int first;
    public String second;

    public InnerBody() {
    }

    public InnerBody(int first, String second) {
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
      InnerBody inner = (InnerBody) o;
      return first == inner.first && Objects.equals(second, inner.second);
    }

    @Override
    public int hashCode() {
      return Objects.hash(first, second);
    }

    @Override
    public String toString() {
      return "InnerBody{" + "first=" + first + ", second='" + second + '\'' + '}';
    }
  }

  @AutoForm
  public static class OuterBody {
    public String var;
    @AutoForm.Kind(FieldKind.Body)
    public InnerBody inner;

    public OuterBody() {

    }

    public OuterBody(String var, InnerBody inner) {
      this.var = var;
      this.inner = inner;
    }

    @Override
    public String toString() {
      return "OuterBody{" + "var='" + var + '\'' + ", inner=" + inner + '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      OuterBody outerBody = (OuterBody) o;
      return Objects.equals(var, outerBody.var) && Objects.equals(inner, outerBody.inner);
    }

    @Override
    public int hashCode() {
      return Objects.hash(var, inner);
    }
  }

  @AutoForm(subTypes = {@AutoForm.Type(AbsLaneClass.class)})
  public static abstract class AbsNodeClass implements HostIFace {
    public String node;

    public AbsNodeClass() {

    }

    public AbsNodeClass(String node) {
      this.node = node;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      AbsNodeClass that = (AbsNodeClass) o;
      return Objects.equals(node, that.node);
    }

    @Override
    public int hashCode() {
      return Objects.hash(node);
    }

    @Override
    public String toString() {
      return "AbsNodeClass{" + "node='" + node + '\'' + '}';
    }
  }

  @AutoForm(subTypes = @AutoForm.Type(ValueClass.class))
  public static abstract class AbsLaneClass extends AbsNodeClass {
    public String lane;

    public AbsLaneClass() {

    }

    public AbsLaneClass(String lane, String node) {
      super(node);
      this.lane = lane;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      if (!super.equals(o)) {
        return false;
      }
      AbsLaneClass that = (AbsLaneClass) o;
      return Objects.equals(lane, that.lane);
    }

    @Override
    public int hashCode() {
      return Objects.hash(super.hashCode(), lane);
    }

    @Override
    public String toString() {
      return "AbsLaneClass{" + "lane='" + lane + '\'' + '}';
    }
  }

  @AutoForm
  public static class ValueClass extends AbsLaneClass {
    public String host;
    @AutoForm.Kind(FieldKind.Body)
    public int value;

    public ValueClass() {

    }

    public ValueClass(int value, String lane, String node, String host) {
      super(lane, node);
      this.value = value;
      this.host = host;
    }

    @Override
    public String host() {
      return host;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      if (!super.equals(o)) {
        return false;
      }
      ValueClass that = (ValueClass) o;
      return value == that.value && Objects.equals(host, that.host);
    }

    @Override
    public int hashCode() {
      return Objects.hash(super.hashCode(), value, host);
    }

    @Override
    public String toString() {
      return "ValueClass{" + "value=" + value + ", host='" + host + '\'' + '}';
    }
  }

  @AutoForm
  public static class GenericClassSlot<G> {
    public String node;
    public String lane;
    public G valueA;
    public G valueB;

    public GenericClassSlot() {

    }

    public GenericClassSlot(String node, String lane, G valueA, G valueB) {
      this.node = node;
      this.lane = lane;
      this.valueA = valueA;
      this.valueB = valueB;
    }

    @Override
    public String toString() {
      return "GenericClassSlot{" + "node='" + node + '\'' + ", lane='" + lane + '\'' + ", valueA=" + valueA + ", valueB=" + valueB + '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      GenericClassSlot<?> that = (GenericClassSlot<?>) o;
      return Objects.equals(node, that.node) && Objects.equals(lane, that.lane) && Objects.equals(valueA, that.valueA) && Objects.equals(valueB, that.valueB);
    }

    @Override
    public int hashCode() {
      return Objects.hash(node, lane, valueA, valueB);
    }
  }

  @AutoForm
  public static class MapList<M extends Map<? extends CharSequence, ? extends Number>, G extends Collection<M>> {
    public G list;

    public MapList() {

    }

    public MapList(G list) {
      this.list = list;
    }

    @Override
    public String toString() {
      return "MapList{" + "list=" + list + '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      MapList<?, ?> mapList = (MapList<?, ?>) o;
      return Objects.equals(list, mapList.list);
    }

    @Override
    public int hashCode() {
      return Objects.hash(list);
    }
  }

  @AutoForm
  public static class GenericArrayClass<N extends Number> {
    public N[] numbers;

    public GenericArrayClass() {

    }

    public GenericArrayClass(N[] numbers) {
      this.numbers = numbers;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      GenericArrayClass<?> that = (GenericArrayClass<?>) o;
      return Arrays.equals(numbers, that.numbers);
    }

    @Override
    public int hashCode() {
      return Arrays.hashCode(numbers);
    }

    @Override
    public String toString() {
      return "GenericArrayClass{" + "numbers=" + Arrays.toString(numbers) + '}';
    }
  }

  @AutoForm
  public static class ValueFieldClass {
    public Value value;

    public ValueFieldClass() {

    }

    public ValueFieldClass(Value value) {
      this.value = value;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      ValueFieldClass that = (ValueFieldClass) o;
      return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
      return Objects.hash(value);
    }

    @Override
    public String toString() {
      return "ValueFieldClass{" + "value=" + value + '}';
    }
  }

  @AutoForm
  public static class GenericHeader<N extends Number> {
    public String node;
    public String lane;
    @AutoForm.Kind(FieldKind.Header)
    public N value;

    public GenericHeader() {

    }

    public GenericHeader(String node, String lane, N value) {
      this.node = node;
      this.lane = lane;
      this.value = value;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      GenericHeader<?> that = (GenericHeader<?>) o;
      return Objects.equals(node, that.node) && Objects.equals(lane, that.lane) && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
      return Objects.hash(node, lane, value);
    }

    @Override
    public String toString() {
      return "GenericHeader{" + "node='" + node + '\'' + ", lane='" + lane + '\'' + ", value='" + value + '\'' + '}';
    }
  }

  @AutoForm
  public static class GenericHeaderBody<N extends Number> {
    public String node;
    public String lane;
    @AutoForm.Kind(FieldKind.HeaderBody)
    public N value;

    public GenericHeaderBody() {

    }

    public GenericHeaderBody(String node, String lane, N value) {
      this.node = node;
      this.lane = lane;
      this.value = value;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      GenericHeaderBody<?> that = (GenericHeaderBody<?>) o;
      return Objects.equals(node, that.node) && Objects.equals(lane, that.lane) && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
      return Objects.hash(node, lane, value);
    }

    @Override
    public String toString() {
      return "GenericHeaderBody{" + "node='" + node + '\'' + ", lane='" + lane + '\'' + ", value='" + value + '\'' + '}';
    }
  }

  @AutoForm
  public static class GenericAttr<N extends Number> {
    public String node;
    public String lane;
    @AutoForm.Kind(FieldKind.Attr)
    public N value;

    public GenericAttr() {

    }

    public GenericAttr(String node, String lane, N value) {
      this.node = node;
      this.lane = lane;
      this.value = value;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      GenericAttr<?> that = (GenericAttr<?>) o;
      return Objects.equals(node, that.node) && Objects.equals(lane, that.lane) && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
      return Objects.hash(node, lane, value);
    }

    @Override
    public String toString() {
      return "GenericAttr{" + "node='" + node + '\'' + ", lane='" + lane + '\'' + ", value='" + value + '\'' + '}';
    }
  }

  @AutoForm
  public static class GenericBody<N extends Number> {
    public String node;
    public String lane;
    @AutoForm.Kind(FieldKind.Body)
    public N value;

    public GenericBody() {

    }

    public GenericBody(String node, String lane, N value) {
      this.node = node;
      this.lane = lane;
      this.value = value;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      GenericBody<?> that = (GenericBody<?>) o;
      return Objects.equals(node, that.node) && Objects.equals(lane, that.lane) && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
      return Objects.hash(node, lane, value);
    }

    @Override
    public String toString() {
      return "GenericBody{" + "node='" + node + '\'' + ", lane='" + lane + '\'' + ", value='" + value + '\'' + '}';
    }
  }

}