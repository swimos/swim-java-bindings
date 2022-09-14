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

public class Slot extends Item {
  private final Value key;
  private final Value value;

  public Slot(Value key, Value value) {
    this.key=key;
    this.value=value;
  }

  @Override
  public String toString() {
    return "Slot{" +
        "key=" + key +
        ", value=" + value +
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
    Slot slot = (Slot) o;
    return Objects.equals(key, slot.key) && Objects.equals(value, slot.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(key, value);
  }

  @Override
  public boolean isSlot() {
    return true;
  }

  public Value getKey() {
    return key;
  }

  public Value getValue() {
    return value;
  }
}
