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
