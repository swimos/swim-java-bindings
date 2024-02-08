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
@AutoForm.Tag("remove")
public class Remove<K> extends MapMessage {
  @AutoForm.Kind(FieldKind.Header)
  public K key;

  public Remove() {

  }

  public Remove(K key) {
    this.key = key;
  }

  @Override
  public String toString() {
    return "Remove{" +
        "key=" + key +
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
    Remove<?> remove = (Remove<?>) o;
    return Objects.equals(key, remove.key);
  }

  @Override
  public int hashCode() {
    return Objects.hash(key);
  }

  @Override
  public boolean isRemove() {
    return true;
  }
}
