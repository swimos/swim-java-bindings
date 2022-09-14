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

import ai.swim.structure.value.num.NumberValue;

import java.math.BigDecimal;
import java.math.BigInteger;

public abstract class Item {

  public static Item of(Value value) {
    return new ValueItem(value);
  }

  public static Item of(Value key, Value value){
    return new Slot(key,value);
  }

  public static Item valueItem(int value) {
    return Item.of(NumberValue.of(value));
  }

  public static Item valueItem(long value) {
    return Item.of(NumberValue.of(value));
  }

  public static Item valueItem(float value) {
    return Item.of(NumberValue.of(value));
  }

  public static Item valueItem(double value) {
    return Item.of(NumberValue.of(value));
  }

  public static Item valueItem(boolean value) {
    return Item.of(new Bool(value));
  }

  public static Item valueItem(BigInteger value) {
    return Item.of(NumberValue.of(value));
  }

  public static Item valueItem(BigDecimal value) {
    return Item.of(NumberValue.of(value));
  }

  public static Item valueItem(byte[] value) {
    return Item.of(new Blob(value));
  }

  public static Item valueItem(String value) {
    return Item.of(new Text(value));
  }

  public boolean isSlot() {
    return false;
  }
}
