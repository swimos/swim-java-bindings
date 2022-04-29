package ai.swim.structure.reflect;

import java.util.Arrays;
import java.util.Objects;

public class MapLayout extends BaseClassLayout {

  private final TypeLayout keyType;
  private final TypeLayout valueType;

  public MapLayout(Class<?> binding, Generics generics, TypeLayout keyType, TypeLayout valueType, TypeLayout[] superInterfaces, TypeLayout superClass) {
    super(binding, generics, superClass, superInterfaces);
    this.keyType = keyType;
    this.valueType = valueType;
  }

  public static TypeLayout of(Class<?> clazz, Generics generics, TypeLayout[] superInterfaces, TypeLayout superClass) {
    if (generics.count() != 2) {
      System.out.println(generics);
      System.exit(1);
    }

    TypeLayout keyType = generics.get(0).type;
    TypeLayout valueType = generics.get(1).type;

    return new MapLayout(clazz, generics, keyType, valueType, superInterfaces, superClass);
  }

  @Override
  public String toString() {
    return "MapLayout{" +
        "superClass=" + superClass +
        ", interfaces=" + Arrays.toString(interfaces) +
        ", generics=" + generics +
        ", keyType=" + keyType +
        ", valueType=" + valueType +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof MapLayout)) return false;
    if (!super.equals(o)) return false;
    MapLayout mapLayout = (MapLayout) o;
    return keyType.equals(mapLayout.keyType) && valueType.equals(mapLayout.valueType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), keyType, valueType);
  }
}
