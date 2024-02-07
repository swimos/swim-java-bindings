package ai.swim.api.protocol;

import ai.swim.structure.annotations.AutoForm;
import ai.swim.structure.annotations.FieldKind;
import java.util.Objects;

@AutoForm
@AutoForm.Tag("update")
public class Update<K, V> extends MapMessage {
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
    Update<?, ?> update = (Update<?, ?>) o;
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
