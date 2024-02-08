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

package ai.swim.recon.event.number;

import ai.swim.recon.event.ReadEvent;
import ai.swim.recon.event.ReadEventVisitor;
import java.util.Objects;

public class ReadIntValue extends ReadEvent {
  private final Integer value;

  public ReadIntValue(Integer value) {
    this.value = value;
  }

  @Override
  public boolean isReadInt() {
    return true;
  }

  @Override
  public <O> O visit(ReadEventVisitor<O> visitor) {
    return visitor.visitInt(value);
  }

  public Integer getValue() {
    return value;
  }

  @Override
  public boolean isPrimitive() {
    return true;
  }

  @Override
  public String toString() {
    return "ReadIntValue{" +
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
    if (!super.equals(o)) {
      return false;
    }
    ReadIntValue that = (ReadIntValue) o;
    return Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }
}
