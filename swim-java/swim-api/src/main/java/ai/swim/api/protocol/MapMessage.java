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

package ai.swim.api.protocol;

import ai.swim.structure.annotations.AutoForm;

@AutoForm(subTypes = {
    @AutoForm.Type(Update.class),
    @AutoForm.Type(Remove.class),
    @AutoForm.Type(Clear.class),
    @AutoForm.Type(Take.class),
    @AutoForm.Type(Drop.class),
})
public abstract class MapMessage {
  public static <K, V> Update<K, V> update(K key, V value) {
    return new Update<>(key, value);
  }

  public static <K> Remove<K> remove(K key) {
    return new Remove<>(key);
  }

  public static Clear clear() {
    return new Clear();
  }

  public static Take take(int n) {
    return new Take(n);
  }

  public static Drop drop(int n) {
    return new Drop(n);
  }

  public boolean isUpdate() {
    return false;
  }

  public boolean isRemove() {
    return false;
  }

  public boolean isClear() {
    return false;
  }

  public boolean isTake() {
    return false;
  }

  public boolean isDrop() {
    return false;
  }
}
