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

package ai.swim.structure.value;

import ai.swim.structure.value.num.NumberValue;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

public abstract class Value {
  public static Value extant() {
    return Extant.extant();
  }

  public static NumberValue of(int value) {
    return NumberValue.of(value);
  }

  public static NumberValue of(long value) {
    return NumberValue.of(value);
  }

  public static NumberValue of(float value) {
    return NumberValue.of(value);
  }

  public static NumberValue of(double value) {
    return NumberValue.of(value);
  }

  public static Bool of(boolean value) {
    return new Bool(value);
  }

  public static NumberValue of(BigInteger value) {
    return NumberValue.of(value);
  }

  public static NumberValue of(BigDecimal value) {
    return NumberValue.of(value);
  }

  public static Blob of(byte[] value) {
    return new Blob(value);
  }

  public static Text of(String value) {
    return new Text(value);
  }

  public static Record record(int numAttrs, int numItems) {
    return new Record(numAttrs, numItems);
  }

  public static Record of(List<Attr> attrs, List<Item> items) {
    return Record.of(attrs, items);
  }

  public static Record of(Attr attr, Item... items) {
    return Record.of(List.of(attr), List.of(items));
  }

  public static Record of(String attr, Item... items) {
    return Record.of(List.of(Value.ofAttr(attr)), List.of(items));
  }

  public static Record ofAttrs(List<Attr> attrs) {
    return Record.ofAttrs(attrs);
  }

  public static Record ofAttrs(Attr... attrs) {
    return Record.ofAttrs(List.of(attrs));
  }

  public static Record ofItems(List<Item> items) {
    return Record.ofItems(items);
  }

  public static Record ofItems(Item... items) {
    return Record.ofItems(List.of(items));
  }

  public static Attr ofAttr(String key) {
    return new Attr(new Text(key), Value.extant());
  }

  public static Attr ofAttr(String key, Value value) {
    return new Attr(new Text(key), value);
  }

  public static Item ofItem(Value value) {
    return new ValueItem(value);
  }

  public static Item ofItem(Value key, Value value) {
    return new Slot(key, value);
  }

  public boolean isRecord() {
    return false;
  }

  public boolean isExtant() {
    return false;
  }

  public boolean isPrimitive() {
    return false;
  }

  @Override
  public abstract boolean equals(Object obj);

  @Override
  public abstract String toString();


}
