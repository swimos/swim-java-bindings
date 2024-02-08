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

package ai.swim.structure.value.num;

import ai.swim.structure.writer.PrimitiveWriter;
import java.math.BigDecimal;
import java.util.Objects;

public class NumberBigDecimal extends NumberValue {
  private final BigDecimal value;

  public NumberBigDecimal(BigDecimal value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return "NumBigDecimal{" +
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
    NumberBigDecimal that = (NumberBigDecimal) o;
    return Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  protected <T> T writePrimitive(PrimitiveWriter<T> writer) {
    return writer.writeBigDecimal(value);
  }
}
