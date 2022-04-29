package ai.swim.structure.reflect;

import java.lang.reflect.Type;
import java.util.*;

public class BaseClassLayout extends TypeLayout {
  protected final TypeLayout superClass;
  protected final TypeLayout[] interfaces;
  protected final Generics generics;

  public BaseClassLayout(Class<?> binding, Generics generics, TypeLayout superClass, TypeLayout[] interfaces) {
    super(binding);
    this.generics = generics;
    this.superClass = superClass;
    this.interfaces = interfaces;
  }

  public static TypeLayout resolveSuperClass(Class<?> clazz, Generics generics) {
    Type superClass = clazz.getGenericSuperclass();
    if (superClass == null) {
      return null;
    }

    return TypeLayoutFactory.getInstance().forType(superClass, generics);
  }

  public static TypeLayout refineStdClass(Class<?> clazz, Generics generics, TypeLayout[] superInterfaces, TypeLayout superClass) {
    if (clazz == Map.class) {
      return MapLayout.of(clazz, generics, superInterfaces, superClass);
    }

    if (clazz == Collection.class) {
//      return CollectionLayout.of(clazz, generics,superInterfaces, superClass);
    }

    if (clazz == HashMap.class) {

    }

    if (clazz == HashSet.class) {

    }

    if (clazz == List.class) {

    }

    return null;
  }

  public static TypeLayout reflectLayout(Generics generics, TypeLayout[] superInterfaces, TypeLayout superClass) {
    return null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof BaseClassLayout)) return false;
    if (!super.equals(o)) return false;
    BaseClassLayout that = (BaseClassLayout) o;

    return Objects.equals(superClass, that.superClass) && Arrays.equals(interfaces, that.interfaces) && Objects.equals(generics, that.generics);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(super.hashCode(), superClass, generics);
    result = 31 * result + Arrays.hashCode(interfaces);
    return result;
  }
}
