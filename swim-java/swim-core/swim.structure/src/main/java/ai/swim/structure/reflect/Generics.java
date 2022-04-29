package ai.swim.structure.reflect;

import java.util.Arrays;
import java.util.Objects;

public class Generics {

  public static final Generics EMPTY = new Generics(new GenericParameter[0]);

  private final GenericParameter[] parameters;

  public Generics(GenericParameter[] parameters) {
    this.parameters = parameters;
  }

  public Generics(String name, TypeLayout layout) {
    this.parameters = new GenericParameter[]{new GenericParameter(name, layout)};
  }

  public Generics(String name1, TypeLayout layout1, String name2, TypeLayout layout2) {
    this.parameters = new GenericParameter[]{
        new GenericParameter(name1, layout1),
        new GenericParameter(name2, layout2),
    };
  }

  public int count() {
    return this.parameters.length;
  }

  public GenericParameter get(int i) {
    if (i > parameters.length) {
      throw new IndexOutOfBoundsException("Attempted to access a generic parameter at index " + i +" for length " + parameters.length);
    }

    return parameters[i];
  }

  public static class GenericParameter {
    final String name;
    final TypeLayout type;

    public GenericParameter(String name, TypeLayout type) {
      this.name = name;
      this.type = type;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof GenericParameter)) return false;
      GenericParameter that = (GenericParameter) o;
      return name.equals(that.name) && type.equals(that.type);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, type);
    }

    @Override
    public String toString() {
      return "GenericParameter{" +
          "name='" + name + '\'' +
          ", type=" + type +
          '}';
    }
  }

  @Override
  public String toString() {
    return "Generics{" +
        "parameters=" + Arrays.toString(parameters) +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Generics)) return false;
    Generics generics = (Generics) o;
    return Arrays.equals(parameters, generics.parameters);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(parameters);
  }
}
