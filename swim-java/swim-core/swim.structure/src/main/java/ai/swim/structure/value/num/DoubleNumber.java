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

package ai.swim.structure.value.num;

import ai.swim.structure.writer.PrimitiveWriter;

import java.util.Objects;

public class DoubleNumber extends NumberValue {
  private final double value;

  public DoubleNumber(double value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return "DoubleNumber{" +
      "value=" + value +
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
    DoubleNumber doubleNumber = (DoubleNumber) o;
    return Double.compare(doubleNumber.value, value) == 0;
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  protected <T> T writePrimitive(PrimitiveWriter<T> writer) {
    return writer.writeDouble(value);
  }
}
