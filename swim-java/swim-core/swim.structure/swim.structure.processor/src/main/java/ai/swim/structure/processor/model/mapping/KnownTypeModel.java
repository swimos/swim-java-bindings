package ai.swim.structure.processor.model.mapping;

import ai.swim.structure.processor.model.*;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.*;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static ai.swim.structure.processor.Utils.isSubType;

/**
 * A model representing a known class type and for resolving known types; primitives, array types, list and map types.
 */
public abstract class KnownTypeModel extends Model {
  private final TypeMapping typeMapping;

  public KnownTypeModel(TypeMirror mirror, Element element, PackageElement packageElement, TypeMapping typeMapping) {
    super(mirror, element, packageElement);
    this.typeMapping = typeMapping;
  }

  /**
   * Attempts to resolve a known library model; primitives, array types, list and map types.
   *
   * @param environment the current processing environment.
   * @param inspector   for resolving nested types.
   * @param element     to inspect.
   * @param type        of the element.
   * @return a resolved model or null if resolution failed.
   * @throws InvalidModelException if the model or a model in its type hierarchy was invalid.
   */
  public static Model getLibraryModel(ProcessingEnvironment environment, ModelInspector inspector, Element element, TypeMirror type) {
    TypeKind typeKind = type.getKind();
    if (typeKind.isPrimitive()) {
      return fromPrimitive(environment, element, type);
    } else if (typeKind == TypeKind.ARRAY) {
      return fromArray(environment, inspector, element, (ArrayType) type);
    } else if (typeKind == TypeKind.DECLARED) {
      return tryFromDeclared(environment, element, inspector, (DeclaredType) type);
    } else {
      return null;
    }
  }

  /**
   * Attempts to resolve an unboxed primitive model from {@code type}.
   *
   * @param environment the current processing environment.
   * @param element     to resolve.
   * @param type        of the element.
   * @return a matching primitive library model.
   * @throws IllegalArgumentException if the type is not a primitive.
   */
  public static Model fromPrimitive(ProcessingEnvironment environment, Element element, TypeMirror type) {
    switch (type.getKind()) {
      case BOOLEAN:
        return PrimitiveLibraryModel.from(environment, type, element, CoreTypeKind.Boolean);
      case BYTE:
        return PrimitiveLibraryModel.from(environment, type, element, CoreTypeKind.Byte);
      case SHORT:
        return PrimitiveLibraryModel.from(environment, type, element, CoreTypeKind.Short);
      case INT:
        return PrimitiveLibraryModel.from(environment, type, element, CoreTypeKind.Integer);
      case LONG:
        return PrimitiveLibraryModel.from(environment, type, element, CoreTypeKind.Long);
      case CHAR:
        return PrimitiveLibraryModel.from(environment, type, element, CoreTypeKind.Character);
      case FLOAT:
        return PrimitiveLibraryModel.from(environment, type, element, CoreTypeKind.Float);
      case DOUBLE:
        return PrimitiveLibraryModel.from(environment, type, element, CoreTypeKind.Double);
      default:
        throw new IllegalArgumentException(type + " is not a primitive");
    }
  }

  /**
   * Attempts to resolve a model from {@code type}. This may be a boxed primitive type, a known library type, a list or
   * map type.
   *
   * @param environment the current processing environment.
   * @param element     to resolve.
   * @param type        of the element.
   * @return a resolved model or null.
   * @throws InvalidModelException if the model or a model in its type hierarchy was invalid.
   */
  private static Model tryFromDeclared(ProcessingEnvironment environment, Element element, ModelInspector inspector, DeclaredType type) {
    Types typeUtils = environment.getTypeUtils();
    Elements elementUtils = environment.getElementUtils();

    if (isSameType(environment, Character.class, type)) {
      return PrimitiveLibraryModel.from(environment, type, element, CoreTypeKind.Character);
    } else if (isSameType(environment, Byte.class, type)) {
      return PrimitiveLibraryModel.from(environment, type, element, CoreTypeKind.Byte);
    } else if (isSameType(environment, Short.class, type)) {
      return PrimitiveLibraryModel.from(environment, type, element, CoreTypeKind.Short);
    } else if (isSameType(environment, Integer.class, type)) {
      return PrimitiveLibraryModel.from(environment, type, element, CoreTypeKind.Integer);
    } else if (isSameType(environment, Long.class, type)) {
      return PrimitiveLibraryModel.from(environment, type, element, CoreTypeKind.Long);
    } else if (isSameType(environment, Float.class, type)) {
      return PrimitiveLibraryModel.from(environment, type, element, CoreTypeKind.Float);
    } else if (isSameType(environment, Double.class, type)) {
      return PrimitiveLibraryModel.from(environment, type, element, CoreTypeKind.Double);
    } else if (isSameType(environment, Boolean.class, type)) {
      return PrimitiveLibraryModel.from(environment, type, element, CoreTypeKind.Boolean);
    } else if (isSameType(environment, String.class, type)) {
      return PrimitiveLibraryModel.from(environment, type, element, CoreTypeKind.String);
    } else if (isSameType(environment, Number.class, type)) {
      return PrimitiveLibraryModel.from(environment, type, element, CoreTypeKind.Number);
    } else if (isSameType(environment, BigInteger.class, type)) {
      return PrimitiveLibraryModel.from(environment, type, element, CoreTypeKind.BigInteger);
    } else if (isSameType(environment, BigDecimal.class, type)) {
      return PrimitiveLibraryModel.from(environment, type, element, CoreTypeKind.BigDecimal);
    } else if (isSubType(environment, type, Collection.class)) {
      List<? extends TypeMirror> typeArguments = type.getTypeArguments();

      if (typeArguments.size() != 1) {
        throw new IllegalArgumentException("Attempted to build a generic type from " + typeArguments.size() + " type parameters where 1 was required");
      }

      UnrolledType unrolledType = unrollType(environment, element, inspector, typeArguments.get(0));
      TypeElement containerType = elementUtils.getTypeElement(List.class.getCanonicalName());
      DeclaredType typedContainer = typeUtils.getDeclaredType(containerType, unrolledType.typeMirror);
      PackageElement packageElement = elementUtils.getPackageElement(List.class.getCanonicalName());

      return new ListLibraryModel(unrolledType.model, element, typedContainer, packageElement);
    } else if (isSubType(environment, type, Map.class)) {
      List<? extends TypeMirror> typeArguments = type.getTypeArguments();

      if (typeArguments.size() != 2) {
        throw new IllegalArgumentException("Attempted to build a generic type from " + typeArguments.size() + " type parameters where 2  required");
      }

      UnrolledType unrolledKey = unrollType(environment, element, inspector, typeArguments.get(0));
      UnrolledType unrolledValue = unrollType(environment, element, inspector, typeArguments.get(1));

      TypeElement containerType = elementUtils.getTypeElement(Map.class.getCanonicalName());
      DeclaredType typedContainer = typeUtils.getDeclaredType(containerType, unrolledKey.typeMirror, unrolledValue.typeMirror);
      PackageElement packageElement = elementUtils.getPackageElement(Map.class.getCanonicalName());

      return new MapLibraryModel(typedContainer, element, unrolledKey.model, unrolledValue.model, packageElement);
    } else {
      return null;
    }
  }

  /**
   * Resolves an array type from {@code arrayType}. If the component type is declared then the component type will be
   * resolved through the inspector, if it is a type var then an untyped model is returned, otherwise, it will be
   * unresolved.
   *
   * @param environment the current processing environment.
   * @param inspector   for resolving the component type if it is declared.
   * @param element     to resolve.
   * @param arrayType   of the element.
   * @return a resolved model.
   * @throws InvalidModelException if the model or a model in its type hierarchy was invalid.
   */
  private static Model fromArray(ProcessingEnvironment environment, ModelInspector inspector, Element element, ArrayType arrayType) {
    TypeMirror componentType = arrayType.getComponentType();
    Model componentModel = getLibraryModel(environment, inspector, element, componentType);

    if (componentModel == null && componentType.getKind() == TypeKind.DECLARED) {
      componentModel = inspector.getOrInspect((TypeElement) componentType, environment);
      return new ArrayLibraryModel(element.asType(), element, componentType, componentModel, componentModel.getDeclaredPackage());
    } else if (componentType.getKind() == TypeKind.TYPEVAR) {
      return new ArrayLibraryModel(element.asType(), element, componentType, new UntypedModel(componentType, element, null), null);
    } else {
      Elements elementUtils = environment.getElementUtils();
      return new ArrayLibraryModel(element.asType(), element, componentType, new UnresolvedModel(componentType, element, elementUtils.getPackageOf(element)), null);
    }
  }

  private static <T> boolean isSameType(ProcessingEnvironment environment, Class<T> clazz, DeclaredType declaredType) {
    Elements elementUtils = environment.getElementUtils();
    TypeElement typeElement = elementUtils.getTypeElement(clazz.getCanonicalName());

    if (typeElement == null) {
      throw new InvalidModelException("Type resolution failure for: " + clazz.getCanonicalName());
    }

    Types typeUtils = environment.getTypeUtils();
    return typeUtils.isSameType(typeElement.asType(), declaredType);
  }

  /**
   * Resolves a type parameter. If {@code typeMirror} is a declared type, then model resolution is attempted on it, if it
   * is a type variable then an {@link UntypedModel} is returned, if it is a wildcard type then it's bounds are removed
   * and a new {@link TypeMirror} is built. I.e, if the {@link TypeMirror} represents {@code List<? super Map<Integer, String}
   * then a new type is built for {@code List<Map<Integer, String>>}.
   *
   * @param environment the current processing environment.
   * @param element     being processed.
   * @param inspector   for resolving the component types.
   * @param typeMirror  to unroll.
   * @return a resolved model.
   * @throws InvalidModelException if the model or a model in its type hierarchy was invalid.
   */
  private static UnrolledType unrollType(ProcessingEnvironment environment, Element element, ModelInspector inspector, TypeMirror typeMirror) {
    switch (typeMirror.getKind()) {
      case DECLARED:
        DeclaredType declaredType = (DeclaredType) typeMirror;
        return new UnrolledType(typeMirror, inspector.getOrInspect(typeMirror, declaredType.asElement(), environment, null));
      case TYPEVAR:
        return new UnrolledType(typeMirror, new UntypedModel(typeMirror, element, null));
      case WILDCARD:
        WildcardType wildcardType = (WildcardType) typeMirror;
        return unrollBoundedType(environment, element, inspector, wildcardType.getExtendsBound(), wildcardType.getSuperBound());
      default:
        throw new AssertionError("Unrolled type: " + typeMirror.getKind());
    }
  }

  /**
   * Removes either an upper or lower bound from a wildcard type if it contains any and returns a {@link UnrolledType}
   * containing a new {@link TypeMirror} for the type and a resolved {@link Model} .
   * <p>
   * I.e, if the {@link TypeMirror} represents {@code List<? super Map<Integer, String} then a new type is built for
   * {@code List<Map<Integer, String>>}.
   *
   * @param environment the current processing environment.
   * @param element     being processed.
   * @param inspector   for resolving the component types.
   * @param lowerBound  {@link TypeMirror} of the lower bound.
   * @param upperBound  {@link TypeMirror} of the upper bound.
   * @return a resolved model.
   * @throws InvalidModelException if the model or a model in its type hierarchy was invalid.
   */
  private static UnrolledType unrollBoundedType(ProcessingEnvironment environment, Element element, ModelInspector inspector, TypeMirror lowerBound, TypeMirror upperBound) {
    TypeMirror bound = lowerBound;

    if (bound == null || bound.getKind() == TypeKind.NULL) {
      bound = upperBound;
    } else if (upperBound != null && upperBound.getKind() != TypeKind.NULL) {
      throw new InvalidModelException("cannot derive a generic field that contains both a lower & upper bound");
    }

    Elements elementUtils = environment.getElementUtils();

    if (bound == null) {
      TypeElement objectTypeElement = elementUtils.getTypeElement(Object.class.getCanonicalName());
      PackageElement packageElement = elementUtils.getPackageElement(Object.class.getCanonicalName());
      return new UnrolledType(objectTypeElement.asType(), new UntypedModel(objectTypeElement.asType(), element, packageElement));
    } else {
      // We need to retype the model here so that the new, unrolled, type is shifted up a level. I.e, if the type that
      // we're unrolling is List<? extends Number> then the new model is List<Number> and that is now the element's
      // type; this will then be propagated up to the callee when they retype the field itself.
      return unrollType(environment, element, inspector, bound);
    }
  }

  @Override
  public boolean isKnownType() {
    return true;
  }

  /**
   * Returns the type that this known type model references.
   */
  public TypeMapping getTypeMapping() {
    return typeMapping;
  }

  private static class UnrolledType {
    public final TypeMirror typeMirror;
    public final Model model;

    public UnrolledType(TypeMirror typeMirror, Model model) {
      this.typeMirror = typeMirror;
      this.model = model;
    }
  }
}
