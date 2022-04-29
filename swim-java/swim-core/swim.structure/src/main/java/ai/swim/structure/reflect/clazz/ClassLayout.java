package ai.swim.structure.reflect.clazz;

import ai.swim.structure.reflect.BaseClassLayout;
import ai.swim.structure.reflect.Generics;
import ai.swim.structure.reflect.TypeLayout;
import ai.swim.structure.reflect.TypeLayoutFactory;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;

public class ClassLayout<C> extends BaseClassLayout {

  private final LinkedHashMap<Class<?>, TypeLayout> fields;
  private final ConstructorLambda<C> constructor;

  public ClassLayout(Class<C> binding, LinkedHashMap<Class<?>, TypeLayout> fields, ConstructorLambda<C> constructor) {
    super(binding, null, null, null);
    this.fields = fields;
    this.constructor = constructor;
  }

  @SuppressWarnings("unchecked")
  public static <C> TypeLayout reflectClass(Class<C> clazz, Generics generics, TypeLayout[] superInterfaces, TypeLayout superClass) {
    TypeLayoutFactory layoutFactory = TypeLayoutFactory.getInstance();
    ConstructorLambda<?> rawConstructor = layoutFactory.getConstructor(clazz);

    if (rawConstructor == null) {
      throw new IllegalArgumentException("Attempted to reflect a class that has not registered a constructor: " + clazz.getCanonicalName());
    }

    ConstructorLambda<C> constructor = (ConstructorLambda<C>) rawConstructor;
    Field[] fields = clazz.getFields();


    return null;
  }
}
