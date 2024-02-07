package ai.swim.api.protocol;

import ai.swim.structure.annotations.AutoForm;
import ai.swim.structure.annotations.FieldKind;
import java.util.Objects;

@AutoForm
@AutoForm.Tag("take")
public class Take extends MapMessage {
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
    Take take = (Take) o;
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
