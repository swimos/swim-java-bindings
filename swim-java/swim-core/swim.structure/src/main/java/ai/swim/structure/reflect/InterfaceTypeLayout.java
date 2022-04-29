package ai.swim.structure.reflect;

import java.lang.reflect.Type;
import java.util.Map;

public class InterfaceTypeLayout extends TypeLayout {

  private static final Class<?> MAP_CLASS = Map.class;

  private static final TypeLayout MAP_LAYOUT = new InterfaceTypeLayout(MAP_CLASS);

  public InterfaceTypeLayout(Class<?> binding) {
    super(binding);
  }

  public static TypeLayout of(Class<?> clazz) {
    return null;
  }

  public static boolean isCommonInterface(Class<?> clazz) {
    if (!clazz.isInterface()) {
      throw new IllegalArgumentException("Class is not an interface: " + clazz.getCanonicalName());
    } else {
      throw new AssertionError();
    }
  }

  public static TypeLayout[] resolveSuperInterfaces(Class<?> clazz, Generics generics) {
    Type[] superInterfaces = clazz.getGenericInterfaces();
    if (superInterfaces.length == 0) {
      return TypeLayout.NO_LAYOUTS;
    }

    TypeLayoutFactory typeLayoutFactory = TypeLayoutFactory.getInstance();
    TypeLayout[] layouts = new TypeLayout[superInterfaces.length];

    for (int i = 0; i < superInterfaces.length; i++) {
      Type type = superInterfaces[i];
      layouts[i] = typeLayoutFactory.forType(type, generics);
    }

    return layouts;
  }

  @Override
  public String toString() {
    return super.toString();
  }
}
