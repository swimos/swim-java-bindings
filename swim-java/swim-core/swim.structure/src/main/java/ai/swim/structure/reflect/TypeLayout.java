package ai.swim.structure.reflect;

import java.util.Objects;

public abstract class TypeLayout {

  protected final static TypeLayout[] NO_LAYOUTS = new TypeLayout[0];

  private final Class<?> binding;

  public TypeLayout(Class<?> binding) {
    this.binding = binding;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TypeLayout that = (TypeLayout) o;
    return binding.equals(that.binding);
  }

  @Override
  public int hashCode() {
    return Objects.hash(binding);
  }

  @Override
  public String toString() {
    return "TypeLayout{" +
        "binding=" + binding +
        '}';
  }
}
