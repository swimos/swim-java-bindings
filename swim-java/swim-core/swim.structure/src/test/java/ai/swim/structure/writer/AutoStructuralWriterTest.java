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
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

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

  @AutoForm(subTypes = {
      @AutoForm.Type(Event.class),
      @AutoForm.Type(Link.class)
  })
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

    public List<C> c;

    public Event() {

    }

    public Event(T i) {
      this.value = i;
    }
  }

  @AutoForm
  public static class Link extends Envelope<String> {

  }

  public static class PropClass {
    private int a;
    private String b;
    private String c;

    public PropClass() {

    }

    public PropClass(int a, String b, String c) {
      this.a = a;
      this.b = b;
      this.c = c;
    }

    public int getA() {
      return a;
    }

    public void setA(int a) {
      this.a = a;
    }

    public String getB() {
      return b;
    }

    public void setB(String b) {
      this.b = b;
    }

    public String getC() {
      return c;
    }

    public void setC(String c) {
      this.c = c;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof PropClass)) {
        return false;
      }
      PropClass propClass = (PropClass) o;
      return a == propClass.a && Objects.equals(b, propClass.b) && Objects.equals(c, propClass.c);
    }

    @Override
    public int hashCode() {
      return Objects.hash(a, b, c);
    }

    @Override
    public String toString() {
      return "PropClass{" +
          "a=" + a +
          ", b='" + b + '\'' +
          ", c='" + c + '\'' +
          '}';
    }
  }

}