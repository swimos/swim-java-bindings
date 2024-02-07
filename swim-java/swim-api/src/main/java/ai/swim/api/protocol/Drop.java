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
