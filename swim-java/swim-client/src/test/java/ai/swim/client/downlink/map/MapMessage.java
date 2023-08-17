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

package ai.swim.client.downlink.map;

import ai.swim.structure.annotations.AutoForm;
import ai.swim.structure.annotations.FieldKind;
import java.util.Objects;

@AutoForm(subTypes = {
    @AutoForm.Type(ai.swim.client.downlink.map.Update.class),
    @AutoForm.Type(ai.swim.client.downlink.map.Remove.class),
    @AutoForm.Type(ai.swim.client.downlink.map.Clear.class),
    @AutoForm.Type(ai.swim.client.downlink.map.Take.class),
    @AutoForm.Type(ai.swim.client.downlink.map.Drop.class),
})
public abstract class MapMessage {
  public static <K, V> ai.swim.client.downlink.map.Update<K, V> update(K key, V value) {
    return new ai.swim.client.downlink.map.Update<>(key, value);
  }

  public static <K> ai.swim.client.downlink.map.Remove<K> remove(K key) {
    return new ai.swim.client.downlink.map.Remove<>(key);
  }

  public static ai.swim.client.downlink.map.Clear clear() {
    return new ai.swim.client.downlink.map.Clear();
  }

  public static ai.swim.client.downlink.map.Take take(int n) {
    return new ai.swim.client.downlink.map.Take(n);
  }

  public static ai.swim.client.downlink.map.Drop drop(int n) {
    return new ai.swim.client.downlink.map.Drop(n);
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

@AutoForm
@AutoForm.Tag("update")
class Update<K, V> extends ai.swim.client.downlink.map.MapMessage {
  public K key;
  @AutoForm.Kind(FieldKind.Body)
  public V value;

  public Update() {

  }

  public Update(K key, V value) {
    this.key = key;
    this.value = value;
  }

  @Override
  public String toString() {
    return "Update{" +
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
    ai.swim.client.downlink.map.Update<?, ?> update = (ai.swim.client.downlink.map.Update<?, ?>) o;
    return Objects.equals(key, update.key) && Objects.equals(value, update.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(key, value);
  }

  @Override
  public boolean isUpdate() {
    return true;
  }
}

@AutoForm
@AutoForm.Tag("remove")
class Remove<K> extends ai.swim.client.downlink.map.MapMessage {
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
    ai.swim.client.downlink.map.Remove<?> remove = (ai.swim.client.downlink.map.Remove<?>) o;
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

@AutoForm
@AutoForm.Tag("clear")
class Clear extends ai.swim.client.downlink.map.MapMessage {
  public Clear() {

  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    return o != null && getClass() == o.getClass();
  }

  @Override
  public boolean isClear() {
    return true;
  }
}

@AutoForm
@AutoForm.Tag("take")
class Take extends ai.swim.client.downlink.map.MapMessage {
  @AutoForm.Kind(FieldKind.HeaderBody)
  public int n;

  public Take() {

  }

  public Take(int n) {
    this.n = n;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ai.swim.client.downlink.map.Take take = (ai.swim.client.downlink.map.Take) o;
    return n == take.n;
  }

  @Override
  public int hashCode() {
    return Objects.hash(n);
  }

  @Override
  public String toString() {
    return "Take{" +
        "n=" + n +
        '}';
  }

  @Override
  public boolean isTake() {
    return true;
  }
}

@AutoForm
@AutoForm.Tag("drop")
class Drop extends ai.swim.client.downlink.map.MapMessage {
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
    ai.swim.client.downlink.map.Drop drop = (ai.swim.client.downlink.map.Drop) o;
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
