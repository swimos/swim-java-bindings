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

package ai.swim.structure.value;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Attr extends Value {

  private final Value value;
  private final Text key;

  public Attr(Text key, Value value) {
    this.key = key;
    this.value = value;
  }

  public static Attr ofAttr(String key) {
    return new Attr(new Text(key), Value.extant());
  }

  public static Attr ofAttr(String key, Value value) {
    return new Attr(new Text(key), value);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Attr attr = (Attr) o;
    return Objects.equals(value, attr.value) && Objects.equals(key, attr.key);
  }

//  @Override
//  public String debug() {
//    return "Attr{" +
//        "value=" + value +
//        ", key=" + key +
//        '}';
//  }

  @Override
  public int hashCode() {
    return Objects.hash(value, key);
  }

  @Override
  public String toString() {
    if (value.isRecord()) {
      Record record = (Record) value;
      Item[] items = record.getItems();

      int attrCount = record.getAttrCount();
      int itemCount = record.getItemCount();

      if (attrCount == 0 && itemCount > 1) {
        return String.format("@%s(%s)", key, Stream.of(items).map(Object::toString).collect(Collectors.joining(",")));
      } else if (attrCount == 0 && itemCount == 1) {
        return String.format("@%s(%s)", key, items[0]);
      } else {
        return String.format("@%s(%s)", key, value);
      }
    } else if (value.isExtant()) {
      return String.format("@%s", key);
    } else {
      return String.format("@%s(%s)", key, value);
    }
  }

  public Text getKey() {
    return key;
  }

  public Value getValue() {
    return value;
  }
}
