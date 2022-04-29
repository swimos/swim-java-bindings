package ai.swim.structure.reflect;

import ai.swim.structure.reflect.clazz.ClassLayout;
import ai.swim.structure.reflect.clazz.ConstructorLambda;

import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.concurrent.ConcurrentHashMap;

public class TypeLayoutFactory {

  private static final TypeLayoutFactory INSTANCE = new TypeLayoutFactory();

  private final ConcurrentHashMap<Class<?>, TypeLayout> layoutCache = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<Class<?>, ConstructorLambda<?>> constructorCache = new ConcurrentHashMap<>();

  public <C> void registerConstructor(Class<C> clazz, ConstructorLambda<C> constructor) {
    constructorCache.put(clazz,constructor);
  }

  private TypeLayoutFactory() {

  }

  public TypeLayout forClass(Class<?> clazz) {
    TypeLayout typeLayout = this.layoutCache.get(clazz);

    if (typeLayout != null) {
      return typeLayout;
    } else {
      return fromClass(clazz);
    }
  }

  public TypeLayout fromParameterizedType(ParameterizedType ty, Generics generics) {
    Class<?> clazz = (Class<?>) ty.getRawType();
    Type[] types = ty.getActualTypeArguments();
    int len = types == null ? 0 : types.length;

    if (clazz == Comparable.class) {
      return new CoreTypeLayout(Comparable.class);
    }

    if (len == 0) {
      generics = Generics.EMPTY;
    } else {
      Generics.GenericParameter[] parameters = new Generics.GenericParameter[len];
      for (int i = 0; i < len; i++) {
        Type type = types[i];
        parameters[i] = new Generics.GenericParameter(type.getTypeName(), forType(type, generics));
      }

      generics = new Generics(parameters);
    }

    return fromClass(clazz, generics);
  }

  public TypeLayout fromClass(Class<?> clazz, Generics generics) {
    TypeLayout typeLayout;

    if (CoreTypeLayout.isCoreType(clazz)) {
      typeLayout = CoreTypeLayout.of(clazz);
      this.layoutCache.put(clazz, typeLayout);
      return typeLayout;
    }

    if (clazz.isArray()) {
      throw new AssertionError("Arrays are unimplemented");
    }

    TypeLayout[] superInterfaces = InterfaceTypeLayout.resolveSuperInterfaces(clazz, generics);
    TypeLayout superClass = null;

    if (!clazz.isInterface()) {
      superClass = BaseClassLayout.resolveSuperClass(clazz, generics);
    }

    typeLayout = BaseClassLayout.refineStdClass(clazz, generics, superInterfaces, superClass);

    if (typeLayout == null) {
      typeLayout = ClassLayout.reflectClass(clazz, generics, superInterfaces, superClass);
    }

    return typeLayout;
  }

  private TypeLayout fromClass(Class<?> clazz) {
    return fromClass(clazz, Generics.EMPTY);
  }

  private TypeLayout introspectTypeLayout(Class<?> clazz) {
    return null;
  }

  public static TypeLayoutFactory getInstance() {
    return TypeLayoutFactory.INSTANCE;
  }

  public TypeLayout forType(Type ty, Generics generics) {
    if (ty instanceof Class<?>) {
      return fromClass((Class<?>) ty, generics);
    }

    if (ty instanceof ParameterizedType) {
      return fromParameterizedType((ParameterizedType) ty, generics);
    }

    if (ty instanceof TypeVariable) {
      return fromTypeVariable((TypeVariable<?>) ty);
    }

    System.out.println(ty);

    throw new AssertionError();
  }

  private TypeLayout fromTypeVariable(TypeVariable<?> ty) {
    return null;
  }

  public ConstructorLambda<?> getConstructor(Class<?> clazz) {
    return constructorCache.get(clazz);
  }
}
