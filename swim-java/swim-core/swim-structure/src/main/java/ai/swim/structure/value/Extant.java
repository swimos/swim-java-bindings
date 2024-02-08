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

import ai.swim.structure.writer.PrimitiveWriter;

public class Extant extends PrimitiveValue {
  private static final Extant VALUE = new Extant();

  public static Value extant() {
    return Extant.VALUE;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    return o != null && getClass() == o.getClass();
  }

  @Override
  public String toString() {
    return "";
  }

  @Override
  public boolean isExtant() {
    return true;
  }

  @Override
  protected <T> T writePrimitive(PrimitiveWriter<T> writer) {
    return writer.writeExtant();
  }
}
