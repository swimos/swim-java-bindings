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

package ai.swim.api.protocol;

import ai.swim.structure.annotations.AutoForm;
import ai.swim.structure.annotations.FieldKind;
import java.util.Objects;

@AutoForm
@AutoForm.Tag("drop")
public class Drop extends MapMessage {
  @AutoForm.Kind(FieldKind.HeaderBody)
  public int n;

  public Drop() {

  }

  public Drop(int n) {
    this.n = n;
  }

  @Override
  public String toString() {
    return "Drop{" +
        "n=" + n +
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
    Drop drop = (Drop) o;
    return n == drop.n;
  }

  @Override
  public int hashCode() {
    return Objects.hash(n);
  }

  @Override
  public boolean isDrop() {
    return true;
  }
}
