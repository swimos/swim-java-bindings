// Copyright 2015-2022 Swim Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package ai.swim.structure.processor.model;

import ai.swim.structure.annotations.AutoForm;
import ai.swim.structure.annotations.FieldKind;
import ai.swim.structure.processor.model.accessor.FieldAccessor;
import ai.swim.structure.processor.model.accessor.MethodAccessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static ai.swim.structure.processor.Utils.accessorFor;
import static ai.swim.structure.processor.Utils.getNoArgConstructor;
import static ai.swim.structure.processor.Utils.isSubType;

/**
 * Core type inspector for deriving a representation of a Java enumeration, class, interface or primitive type that will
 * be used when applying a transformation into a recognizer and/or writer class.
 * <p>
 *
 * <h2>Model Resolution</h2>
 * This class provides model resolution and inspection via two {@code getOrInspect} methods which provides resolution
 * through a {@code TypeElement} which is typically a root processing element in a class or by a {@code TypeMirror} when
 * resolving a field in a class.
 * <p>
 * When a type is being inspected, its superclasses, subclasses, fields and methods are all inspected, traversed and
 * models are derived and stored for possible resolution through another invocation. This enables faster resolution for
 * frequently used types. For example, a class {@code C} may have fields that are declared types of type {@code A} and
 * {@code B}. When {@code C} is inspected, both {@code A} and {@code B} will also be inspected and their respective
 * model derivations will be stored in the inspector for possible access by another model.
 *
 * <h3>Class Resolution</h3>
 * <p>
 * If a class that is being derived has a superclass, then the type's hierarchy is traversed and a flattened
 * representation is derived that contains all of its parents fields and accessors; excluding ignored fields. This
 * places a constraint in the hierarchy that all fields and accessors must be unique. As an example:
 * <pre>
 * {@code
 *  class SuperType {
 *    public String name;
 *  }
 *
 *  class SubType extends SuperType {
 *    public int age;
 *  }
 * }
 * </pre>
 * This is a valid hierarchy as each field is unique. If {@code SubType} also contained a field named {@code name} then
 * model resolution would fail.
 * <p>
 * Once {@code SuperType}'s model has been resolved, it is placed into a map where it can be retrieved by its type
 * mirror.
 * <p>
 *
 * <h3>Inheritance Resolution</h3>
 * The inspector supports inheritance through class abstraction (like Java, it does not support multiple inheritance)
 * and interfaces.
 * <p>
 * When inspecting a class or interface that contains super and/or subtypes, a guard is placed to prevent an infinite
 * resolution cycle between two types. Without this, it's possible for a class's supertype to be inspected and then its
 * subtypes to be inspected and then a resolution cycle is formed. This is observed in this class as a 'skip' field and
 * when a cycle is observed, a resolved model is instead placed into the current model instead of it being inspected.
 * <p>
 * A restriction is enforced in the type hierarchy that a type may not extend another type that is not automatically
 * derived. If this functionality is required, then a form must be manually implemented.
 */
public class ModelInspector {
  private static final Map<String, CoreTypeSpec<?>> CORE_TYPES = new HashMap<>();

  static {
    putType(Character.TYPE, CoreTypeModel.Kind.Character, '\u0000');
    putType(Byte.TYPE, CoreTypeModel.Kind.Byte, (byte) 0);
    putType(Short.TYPE, CoreTypeModel.Kind.Short, (short) 0);
    putType(Integer.TYPE, CoreTypeModel.Kind.Integer, 0);
    putType(Long.TYPE, CoreTypeModel.Kind.Long, 0L);
    putType(Float.TYPE, CoreTypeModel.Kind.Float, 0f);
    putType(Double.TYPE, CoreTypeModel.Kind.Double, 0d);
    putType(Boolean.TYPE, CoreTypeModel.Kind.Boolean, false);
    putType(Character.class, CoreTypeModel.Kind.Character, null);
    putType(Byte.class, CoreTypeModel.Kind.Byte, null);
    putType(Short.class, CoreTypeModel.Kind.Short, null);
    putType(Integer.class, CoreTypeModel.Kind.Integer, null);
    putType(Long.class, CoreTypeModel.Kind.Long, null);
    putType(Float.class, CoreTypeModel.Kind.Float, null);
    putType(Double.class, CoreTypeModel.Kind.Double, null);
    putType(Boolean.class, CoreTypeModel.Kind.Boolean, null);
    putType(Number.class, CoreTypeModel.Kind.Number, null);
    putType(String.class, CoreTypeModel.Kind.String, "");
    putType(BigInteger.class, CoreTypeModel.Kind.BigInteger, null);
    putType(BigDecimal.class, CoreTypeModel.Kind.BigDecimal, null);
  }

  /**
   * Type map between a type mirror and its derived model representation.
   * <p>
   * A type mirror may be a mirror from a {@code TypeElement} or a field's {@code TypeMirror}.
   */
  private final HashMap<TypeMirror, Model> models;

  public ModelInspector() {
    models = new HashMap<>();
  }

  private static <T> void putType(Class<T> clazz, CoreTypeModel.Kind kind, T defaultValue) {
    CORE_TYPES.put(clazz.getCanonicalName(), new CoreTypeSpec<>(clazz, kind, defaultValue));
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
  public static Model getLibraryModel(ProcessingEnvironment environment,
      ModelInspector inspector,
      Element element,
      TypeMirror type) {
    CoreTypeSpec<?> model = CORE_TYPES.get(type.toString());
    if (model != null) {
      return CoreTypeModel.from(environment, type, element, model);
    } else {
      TypeKind typeKind = type.getKind();
      if (typeKind == TypeKind.ARRAY) {
        return fromArray(environment, inspector, element, (ArrayType) type);
      } else if (typeKind == TypeKind.DECLARED) {
        return tryFromDeclared(environment, element, inspector, (DeclaredType) type);
      } else {
        return null;
      }
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
  private static Model tryFromDeclared(ProcessingEnvironment environment,
      Element element,
      ModelInspector inspector,
      DeclaredType type) {
    Types typeUtils = environment.getTypeUtils();
    Elements elementUtils = environment.getElementUtils();

    if (isSubType(environment, type, Collection.class)) {
      List<? extends TypeMirror> typeArguments = type.getTypeArguments();

      if (typeArguments.size() != 1) {
        throw new IllegalArgumentException("Attempted to build a generic type from " + typeArguments.size() + " type parameters where 1 was required");
      }

      UnrolledType unrolledType = unrollType(environment, element, inspector, typeArguments.get(0));
      TypeElement containerType = elementUtils.getTypeElement(List.class.getCanonicalName());
      DeclaredType typedContainer = typeUtils.getDeclaredType(containerType, unrolledType.typeMirror);
      PackageElement packageElement = elementUtils.getPackageElement(List.class.getCanonicalName());

      return new ParameterisedTypeModel(
          typedContainer,
          element,
          packageElement,
          ParameterisedTypeModel.Mapping.List,
          unrolledType.model);
    } else if (isSubType(environment, type, Map.class)) {
      List<? extends TypeMirror> typeArguments = type.getTypeArguments();

      if (typeArguments.size() != 2) {
        throw new IllegalArgumentException("Attempted to build a generic type from " + typeArguments.size() + " type parameters where 2 are required");
      }

      UnrolledType unrolledKey = unrollType(environment, element, inspector, typeArguments.get(0));
      UnrolledType unrolledValue = unrollType(environment, element, inspector, typeArguments.get(1));

      TypeElement containerType = elementUtils.getTypeElement(Map.class.getCanonicalName());
      DeclaredType typedContainer = typeUtils.getDeclaredType(
          containerType,
          unrolledKey.typeMirror,
          unrolledValue.typeMirror);
      PackageElement packageElement = elementUtils.getPackageElement(Map.class.getCanonicalName());

      return new ParameterisedTypeModel(
          typedContainer,
          element,
          packageElement,
          ParameterisedTypeModel.Mapping.Map,
          unrolledKey.model,
          unrolledValue.model);
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
  private static Model fromArray(ProcessingEnvironment environment,
      ModelInspector inspector,
      Element element,
      ArrayType arrayType) {
    TypeMirror componentType = arrayType.getComponentType();
    Model componentModel = getLibraryModel(environment, inspector, element, componentType);

    if (componentModel == null && componentType.getKind() == TypeKind.DECLARED) {
      componentModel = inspector.getOrInspect((TypeElement) componentType, environment);
      return new ArrayLibraryModel(
          element.asType(),
          element,
          componentType,
          componentModel,
          componentModel.getDeclaredPackage());
    } else if (componentType.getKind() == TypeKind.TYPEVAR) {
      return new ArrayLibraryModel(
          element.asType(),
          element,
          componentType,
          new UntypedModel(componentType, element, null),
          null);
    } else {
      Elements elementUtils = environment.getElementUtils();
      return new ArrayLibraryModel(
          element.asType(),
          element,
          componentType,
          new UnresolvedModel(componentType, element, elementUtils.getPackageOf(element)),
          null);
    }
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
  private static UnrolledType unrollType(ProcessingEnvironment environment,
      Element element,
      ModelInspector inspector,
      TypeMirror typeMirror) {
    switch (typeMirror.getKind()) {
      case DECLARED:
        DeclaredType declaredType = (DeclaredType) typeMirror;
        return new UnrolledType(
            typeMirror,
            inspector.getOrInspect(typeMirror, declaredType.asElement(), environment, null));
      case TYPEVAR:
        return new UnrolledType(typeMirror, new UntypedModel(typeMirror, element, null));
      case WILDCARD:
        WildcardType wildcardType = (WildcardType) typeMirror;
        return unrollBoundedType(
            environment,
            element,
            inspector,
            wildcardType.getExtendsBound(),
            wildcardType.getSuperBound());
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
  private static UnrolledType unrollBoundedType(ProcessingEnvironment environment,
      Element element,
      ModelInspector inspector,
      TypeMirror lowerBound,
      TypeMirror upperBound) {
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
      return new UnrolledType(
          objectTypeElement.asType(),
          new UntypedModel(objectTypeElement.asType(), element, packageElement));
    } else {
      // We need to retype the model here so that the new, unrolled, type is shifted up a level. I.e, if the type that
      // we're unrolling is List<? extends Number> then the new model is List<Number> and that is now the element's
      // type; this will then be propagated up to the callee when they retype the field itself.
      return unrollType(environment, element, inspector, bound);
    }
  }

  /**
   * Gets and inspects a {@code Model} for {@code element}.
   *
   * @param element     to be inspected.
   * @param environment the current processing environment.
   * @return a model representation for {@code element}.
   * @throws InvalidModelException if an invalid model has been derived anywhere from this element's type hierarchy.
   */
  public Model getOrInspect(TypeElement element, ProcessingEnvironment environment) {
    return getOrInspect(element.asType(), element, environment, null);
  }

  /**
   * Gets and inspects a {@code Model} for {@code element}.
   * <p>
   * If {@code element} is an element representing a field then {@code type} *must* be the {@code TypeMirror} for the
   * field and not for the {@code element} otherwise, when the model is transformed into a recognizer and/or writer
   * representation an invalid field type will be written.
   *
   * @param type        of the class/enum/interface if {@code element} is a {@code TypeElement} or, if it is a field, then
   *                    the type of the field.
   * @param element     to be inspected.
   * @param environment the current processing environment.
   * @param skip        if this inspection has been triggered through an inheritance resolution call, then skip inspecting the
   *                    element that triggered the resolution. If this is an inheritance resolution call and this is not
   *                    provided then this may cause a {@code StackOverflowException}.
   * @return a model representation for {@code element}.
   * @throws InvalidModelException if an invalid model has been derived anywhere from this element's type hierarchy.
   */
  public Model getOrInspect(TypeMirror type, Element element, ProcessingEnvironment environment, StructuralModel skip) {
    if (element == null) {
      throw new NullPointerException();
    }

    Model model = models.get(type);

    if (model == null) {
      Model derived = inspectElement(type, element, environment, skip);
      models.put(type, derived);
      return derived;
    } else {
      return model;
    }
  }

  /**
   * Gets and inspects a {@code Model} for a class-like element.
   *
   * @param type        of the class/enum.
   * @param element     to be inspected.
   * @param environment the current processing environment.
   * @param skip        if this inspection has been triggered through an inheritance resolution call, then skip inspecting the
   *                    element that triggered the resolution. If this is an inheritance resolution call and this is not
   *                    provided then this may cause a {@code StackOverflowException}.
   * @return a model representation for {@code element}.
   * @throws InvalidModelException    if an invalid model has been derived anywhere from this element's type hierarchy.
   * @throws IllegalArgumentException if a previously resolved model exists for {@code type} that is not a class like model.
   */
  private ClassLikeModel getOrInspectClass(TypeMirror type,
      TypeElement element,
      ProcessingEnvironment environment,
      StructuralModel skip) {
    if (element == null) {
      throw new NullPointerException();
    }

    Model model = models.get(type);

    if (model == null) {
      ClassLikeModel derived = inspectClass(type, element, environment, skip);
      models.put(type, derived);
      return derived;
    } else {
      if (!model.isClassLike()) {
        throw new IllegalArgumentException(String.format("%s is not a class", type));
      }
      return (ClassLikeModel) model;
    }
  }

  /**
   * Resolves a declared type by branching into inspecting a class if {@code element} is a class-like element or
   * inspecting an interface if it is an interface. If resolution fails due to the element not being annotated with
   * {@code AutoForm} then {@code null} is returned.
   *
   * @param type        of the class/enum/interface.
   * @param element     to be inspected.
   * @param environment the current processing environment.
   * @param current     if this inspection has been triggered through an inheritance resolution call, then skip inspecting the
   *                    element that triggered the resolution. If this is an inheritance resolution call and this is not
   *                    provided then this may cause a {@code StackOverflowException}.
   * @return a model representation for {@code element}.
   * @throws InvalidModelException    if an invalid model has been derived anywhere from this element's type hierarchy.
   * @throws IllegalArgumentException if the kind of the element is not class-like or an interface.
   */
  private Model resolveDeclaredType(TypeMirror type,
      TypeElement element,
      ProcessingEnvironment environment,
      StructuralModel current) {
    ElementKind kind = element.getKind();
    if (kind.isClass()) {
      AutoForm annotation = element.getAnnotation(AutoForm.class);
      if (annotation == null) {
        return null;
      } else {
        return inspectClass(type, element, environment, current);
      }
    } else if (kind.isInterface()) {
      AutoForm annotation = element.getAnnotation(AutoForm.class);
      if (annotation == null) {
        return null;
      } else {
        return inspectInterface(element, environment);
      }
    } else {
      throw new InvalidModelException(element + " is not a supported type");
    }
  }

  /**
   * Resolves a declared type by branching into inspecting a class if {@code element} is a class-like element or
   * inspecting an interface if it is an interface. If resolution fails due to the element not being annotated with
   * {@code AutoForm} then an {@code UnresolvedModel} is returned.
   *
   * @param type        of the class/enum/interface.
   * @param element     to be inspected.
   * @param environment the current processing environment.
   * @param current     if this inspection has been triggered through an inheritance resolution call, then skip inspecting the
   *                    element that triggered the resolution. If this is an inheritance resolution call and this is not
   *                    provided then this may cause a {@code StackOverflowException}.
   * @return a model representation for {@code element}.
   * @throws InvalidModelException if an invalid model has been derived anywhere from this element's type hierarchy.
   */
  private Model inspectDeclaredType(TypeMirror type,
      TypeElement element,
      ProcessingEnvironment environment,
      StructuralModel current) {
    Model resolved = resolveDeclaredType(type, element, environment, current);
    if (resolved == null) {
      Elements elementUtils = environment.getElementUtils();
      PackageElement packageElement = elementUtils.getPackageOf(element);
      return new UnresolvedModel(element.asType(), element, packageElement);
    } else {
      return resolved;
    }
  }

  /**
   * Gets and inspects a {@code Model} for {@code element}.
   * <p>
   * If {@code element} is an element representing a field then {@code type} *must* be the {@code TypeMirror} for the
   * field and not for the {@code element} otherwise, when the model is transformed into a recognizer and/or writer
   * representation an invalid field type will be written.
   *
   * @param elementType of the class/enum/interface if {@code element} is a {@code TypeElement} or, if it is a field, then
   *                    the type of the field.
   * @param element     to be inspected.
   * @param environment the current processing environment.
   * @param skipRoot    if this inspection has been triggered through an inheritance resolution call, then skip inspecting the
   *                    element that triggered the resolution. If this is an inheritance resolution call and this is not
   *                    provided then this may cause a {@code StackOverflowException}.
   * @return a model representation for {@code element}. This may be a derived structural model or an unresolved model.
   * @throws InvalidModelException if an invalid model has been derived anywhere from this element's type hierarchy or
   *                               if resolution failed.
   */
  private Model inspectElement(TypeMirror elementType,
      Element element,
      ProcessingEnvironment environment,
      StructuralModel skipRoot) {
    ElementKind kind = element.getKind();
    Model model = getLibraryModel(environment, this, element, elementType);

    if (model != null) {
      return model;
    } else if (kind.isClass() || kind.isInterface()) {
      return inspectDeclaredType(elementType, (TypeElement) element, environment, skipRoot);
    } else if (kind.isField()) {
      VariableElement variableElement = (VariableElement) element;
      TypeMirror fieldType = variableElement.asType();

      TypeKind typeKind = fieldType.getKind();
      model = getLibraryModel(environment, this, element, variableElement.asType());

      if (model != null) {
        return model;
      } else if (typeKind == TypeKind.DECLARED) {
        Element declaredElement = ((DeclaredType) fieldType).asElement();
        Elements elementUtils = environment.getElementUtils();
        TypeElement typeElement = elementUtils.getTypeElement(declaredElement.toString());
        if (typeElement == null) {
          throw new InvalidModelException("Failed to resolve type: " + fieldType);
        } else {
          return inspectDeclaredType(fieldType, typeElement, environment, skipRoot);
        }
      } else if (typeKind == TypeKind.TYPEVAR || typeKind == TypeKind.WILDCARD) {
        Elements elementUtils = environment.getElementUtils();
        PackageElement packageElement = elementUtils.getPackageOf(element);
        // Return an unresolved model for the transformation to decide on how to handle typevars and wildcards.
        // For a recognizer, it will be aligned to an untyped class and for a writer it will perform a runtime lookup of
        // the type or resolution will fail.
        return new UnresolvedModel(fieldType, element, packageElement);
      }
    }

    // This is a bug if the kind has not been handled or the user defined something that isn't supported.
    throw new AssertionError("Unsupported type: " + kind);
  }

  /**
   * Gets and inspects a {@code ClassLikeModel} for {@code element}. If {@code element} contains a direct superclass,
   * then the returned model will contain *all* of the fields in the inheritance tree and the corresponding accessors.
   *
   * @param type        of the class.
   * @param element     to be inspected.
   * @param environment the current processing environment.
   * @param skipSubType if this inspection has been triggered through an inheritance resolution call, then skip inspecting the
   *                    element that triggered the resolution. If this is an inheritance resolution call and this is not
   *                    provided then this may cause a {@code StackOverflowException}.
   * @return a model representation for {@code element}.
   * @throws InvalidModelException if an invalid model has been derived anywhere from this element's type hierarchy.
   */
  private ClassLikeModel inspectClass(TypeMirror type,
      TypeElement element,
      ProcessingEnvironment environment,
      StructuralModel skipSubType) {
    validateNoArgConstructor(element);

    Elements elementUtils = environment.getElementUtils();
    PackageElement declaredPackage = elementUtils.getPackageOf(element);

    ClassLikeModel classModel = new ClassLikeModel(type, element, declaredPackage);

    inspectTag(element);
    inspectClass(classModel, element, environment);

    // The model's fields and methods have now been resolved, and so we can trigger an inheritance resolution call. If
    // the call was performed earlier then the class merge operation would be missing elements.

    if (!element.getKind().equals(ElementKind.ENUM)) {
      inspectSuperclass(element, classModel, environment, skipSubType);
    }

    inspectClassSubTypes(element, classModel, environment);

    return classModel;
  }

  /**
   * Validates the placement and content of an {@code @Tag} if it has been placed on {@code element}.
   *
   * @throws InvalidModelException is the tag is invalid.
   */
  private void inspectTag(TypeElement element) {
    if (element.getKind().equals(ElementKind.ENUM)) {
      if (element.getAnnotation(AutoForm.Tag.class) != null) {
        throw new InvalidModelException(String.format(
            "%s cannot be used on enumerations, only on constants",
            AutoForm.Tag.class.getCanonicalName()));
      }

      List<? extends Element> enclosedElements = element.getEnclosedElements();
      HashSet<String> tags = new HashSet<>();

      for (Element enclosedElement : enclosedElements) {
        if (enclosedElement.getKind().equals(ElementKind.ENUM_CONSTANT)) {
          AutoForm.Tag currentTag = enclosedElement.getAnnotation(AutoForm.Tag.class);
          String constantTag;
          if (currentTag != null && !currentTag.value().isBlank()) {
            if (!currentTag.value().chars().allMatch(Character::isLetterOrDigit)) {
              throw new InvalidModelException(String.format("invalid characters in tag: '%s'", currentTag.value()));
            }

            constantTag = currentTag.value();
          } else {
            constantTag = enclosedElement.toString();
          }

          if (!tags.add(constantTag)) {
            throw new InvalidModelException(String.format("contains a duplicate tag: '%s'", constantTag));
          }
        }
      }
    } else {
      AutoForm.Tag tag = element.getAnnotation(AutoForm.Tag.class);

      if (tag != null && !tag.value().isBlank()) {
        if (!tag.value().chars().allMatch(Character::isLetterOrDigit)) {
          throw new InvalidModelException(String.format("invalid characters in tag: '%s'", tag.value()));
        }
      }
    }
  }

  /**
   * Inspects {@code rootElement}'s fields and methods. Ensuring that each field's type can be resolved and that the
   * suitable accessors exists; a field must either be public or have a getter and setter.
   *
   * @param classModel  the model being built.
   * @param rootElement to be inspected.
   * @param environment the current processing environment.
   * @throws InvalidModelException if an invalid model has been derived anywhere from this element's type hierarchy or
   *                               if the Recon field schema is invalid.
   */
  private void inspectClass(ClassLikeModel classModel, TypeElement rootElement, ProcessingEnvironment environment) {
    Types typeUtils = environment.getTypeUtils();
    List<VariableElement> variables = new ArrayList<>();

    for (Element element : rootElement.getEnclosedElements()) {
      switch (element.getKind()) {
        case FIELD:
          variables.add((VariableElement) element);
          break;
        case METHOD:
          classModel.addMethod((ExecutableElement) element);
          break;
      }
    }

    Manifest manifest = new Manifest();

    for (VariableElement variable : variables) {
      boolean isPublic = variable.getModifiers().contains(Modifier.PUBLIC);
      TypeMirror variableType = variable.asType();
      FieldKind fieldKind = getFieldKind(variable);
      Name variableName = variable.getSimpleName();

      if (variable.getAnnotation(AutoForm.Ignore.class) != null) {
        continue;
      }

      if (typeUtils.isSameType(rootElement.asType(), variableType)) {
        throw new InvalidModelException("Recursive types are not supported by the form annotation processor.");
      }

      manifest.validate(fieldKind);

      Model fieldModel = getOrInspect(variableType, variable, environment, classModel);

      if (isPublic) {
        classModel.addField(new FieldModel(
            new FieldAccessor(variableName.toString()),
            fieldModel,
            variable,
            fieldKind));
      } else {
        ExecutableElement setter = accessorFor(
            classModel.getMethods(),
            "set",
            variableName,
            AutoForm.Setter.class,
            (anno) -> anno.value().equals(variableName.toString()));
        ExecutableElement getter = accessorFor(
            classModel.getMethods(),
            "get",
            variableName,
            AutoForm.Getter.class,
            (anno) -> anno.value().equals(variableName.toString()));

        if (rootElement.getKind() != ElementKind.ENUM) {
          validateSetter(setter, variableName, variableType, environment);
        }

        validateGetter(getter, variableName, variableType, environment);
        classModel.addField(new FieldModel(new MethodAccessor(setter, getter), fieldModel, variable, fieldKind));
      }
    }
  }

  /**
   * Inspects {@code rootElement}'s subtypes if the element has its {@code subTypes} field populated and that the
   * annotation has been correctly used; that it has not been placed on an enumeration or the class is abstract and has
   * no subtypes.
   * <p>
   * This method triggers an inheritance resolution call whereby {@code classModel} will be ignored during the
   * resolution. As such, {@code classModel} must be fully resolved to ensure that other types resolve correctly.
   *
   * @param classModel  the model being built.
   * @param rootElement to be inspected.
   * @param environment the current processing environment.
   * @throws InvalidModelException if an invalid model has been derived anywhere from this element's type hierarchy or
   *                               if class is missing subtypes, and it is abstract, or if the annotation has been
   *                               placed on an enumeration.
   */
  private void inspectClassSubTypes(Element rootElement, ClassLikeModel classModel, ProcessingEnvironment environment) {
    boolean isAbstract = rootElement.getModifiers().stream().anyMatch(modifier -> modifier.equals(Modifier.ABSTRACT));

    AutoForm autoForm = rootElement.getAnnotation(AutoForm.class);
    if (autoForm == null) {
      // This is fine as it's a class that has an abstract supertype that we can't deserialize directly into.
      return;
    }

    AutoForm.Type[] subTypes = autoForm.subTypes();

    if (!isAbstract && subTypes.length == 0) {
      return;
    } else if (isAbstract && subTypes.length == 0) {
      throw new InvalidModelException("Class missing subtypes");
    } else if (rootElement.getKind() == ElementKind.ENUM) {
      throw new InvalidModelException("Enumerations do not support subtyping");
    }

    List<Model> subTypeElements = inspectSubTypes(rootElement, classModel, environment, Set.of(subTypes));
    classModel.setAbstract(isAbstract);
    classModel.setSubTypes(subTypeElements);
  }

  /**
   * Inspects a set of subtypes for a structural model (class or interface) and returns a resolved list of models.
   * <p>
   * This method triggers an inheritance resolution call whereby {@code classModel} will be ignored during the
   * resolution. As such, {@code classModel} must be fully resolved to ensure that other types resolve correctly.
   *
   * @param rootElement to be inspected.
   * @param model       the model being built.
   * @param environment the current processing environment.
   * @throws InvalidModelException if an invalid model has been derived anywhere from this element's type hierarchy or
   *                               an invalid inheritance tree has been defined (a user defined a subtype that does not
   *                               extend from {@code rootElement} or it is the same class).
   */
  private List<Model> inspectSubTypes(Element rootElement,
      StructuralModel model,
      ProcessingEnvironment environment,
      Set<AutoForm.Type> subTypes) {
    Types typeUtils = environment.getTypeUtils();
    Set<Model> subTypeElements = new HashSet<>(subTypes.size());
    TypeMirror rootType = rootElement.asType();

    for (AutoForm.Type subType : subTypes) {
      Element subTypeElement;
      TypeMirror subTypeMirror;

      try {
        // this will always throw
        //noinspection ResultOfMethodCallIgnored
        subType.value();
        throw new AssertionError("Failed to access target class for: " + rootElement);
      } catch (MirroredTypeException e) {
        subTypeMirror = e.getTypeMirror();
        subTypeElement = typeUtils.asElement(e.getTypeMirror());
      }

      if (typeUtils.isSameType(subTypeMirror, rootType)) {
        throw new InvalidModelException("Subtype cannot be root type");
      }

      if (!typeUtils.isSubtype(typeUtils.erasure(subTypeMirror), typeUtils.erasure(rootType))) {
        throw new InvalidModelException(String.format("%s is not a subtype of %s", subTypeMirror, rootType));
      }

      ElementKind kind = subTypeElement.getKind();
      boolean validSuperType = typeUtils
          .directSupertypes(subTypeMirror)
          .stream()
          .anyMatch(elem -> typeUtils.isSameType(elem, rootType));

      if (rootElement.getKind() == ElementKind.CLASS && !validSuperType) {
        throw new InvalidModelException(String.format("%s is not a direct supertype of %s", rootType, subTypeMirror));
      }

      if (kind.isClass() || kind.isInterface()) {
        subTypeElements.add(getOrInspect(subTypeMirror, subTypeElement, environment, model));
      } else {
        throw new InvalidModelException(String.format("%s must be a class or interface", subTypeElement));
      }
    }

    return new ArrayList<>(subTypeElements);
  }

  /**
   * This method is intended to be used after resolving an accessor using the method {@code accessorFor} and validates
   * that the setter is valid for a field of name {@code name}. A setter is considered to be valid if it is not {@code null},
   * it is public and it contains a single parameter that has a matching type to the field.
   *
   * @param setter       method.
   * @param name         the name of the field.
   * @param expectedType the expected type of the field.
   * @param environment  the current processing environment.
   * @throws InvalidModelException if any of the constraints are violated.
   */
  private void validateSetter(ExecutableElement setter,
      Name name,
      TypeMirror expectedType,
      ProcessingEnvironment environment) {
    Types typeUtils = environment.getTypeUtils();

    if (setter == null) {
      throw new InvalidModelException("Private field: '" + name + "' has no setter");
    }

    if (!setter.getModifiers().contains(Modifier.PUBLIC)) {
      throw new InvalidModelException("Private field: '" + name + "' has no public setter");
    }

    List<? extends VariableElement> parameters = setter.getParameters();
    if (parameters.size() != 1) {
      throw new InvalidModelException("expected a setter for field '" + name + "' that takes one parameter");
    }

    VariableElement variableElement = parameters.get(0);

    if (!typeUtils.isSameType(variableElement.asType(), expectedType)) {
      String cause = String.format("Expected type: '%s', found: '%s'", variableElement.asType(), expectedType);
      throw new InvalidModelException("setter for field '" + name + "' accepts an incorrect type. Cause: " + cause);
    }
  }

  /**
   * This method is intended to be used after resolving an accessor using the method {@code accessorFor} and validates
   * that the getter is valid for a field of name {@code name}. A getter is considered to be valid if it is not {@code null},
   * it is public and the return type matches the type of the field.
   *
   * @param getter       method.
   * @param name         the name of the field.
   * @param expectedType the expected type of the field.
   * @param environment  the current processing environment.
   * @throws InvalidModelException if any of the constraints are violated.
   */
  private void validateGetter(ExecutableElement getter,
      Name name,
      TypeMirror expectedType,
      ProcessingEnvironment environment) {
    Types typeUtils = environment.getTypeUtils();

    if (getter == null) {
      throw new InvalidModelException("Private field: '" + name + "' has no getter");
    }

    if (!getter.getModifiers().contains(Modifier.PUBLIC)) {
      throw new InvalidModelException("Private field: '" + name + "' has no public getter");
    }

    if (!getter.getParameters().isEmpty()) {
      throw new InvalidModelException("getter for field '" + name + "' should have no parameter");
    }

    if (!typeUtils.isSameType(getter.getReturnType(), expectedType)) {
      throw new InvalidModelException("getter for field '" + name + "' returns an incorrect type");
    }
  }

  /**
   * Returns the corresponding Recon {@code Kind} of the field. If the element is annotated with {@code Kind} then the
   * value is returned, otherwise, defaults to a slot.
   */
  private FieldKind getFieldKind(Element element) {
    AutoForm.Kind kind = element.getAnnotation(AutoForm.Kind.class);
    if (kind == null) {
      return FieldKind.Slot;
    } else {
      return kind.value();
    }
  }

  /**
   * Inspects the direct supertype of {@code element} and if on exists, then its fields are merged into {@code classModel}.
   * If this method has been triggered through an inheritance resolution call and the direct supertype is {@code skipRoot}
   * then its contents are merged into {@code classModel} and as such, it must be a fully resolved model.
   *
   * @param element     to be inspected.
   * @param classModel  the model being built.
   * @param environment the current processing environment.
   * @throws InvalidModelException if an invalid model has been derived anywhere from this element's type hierarchy.
   */
  private void inspectSuperclass(TypeElement element,
      ClassLikeModel classModel,
      ProcessingEnvironment environment,
      StructuralModel skipRoot) {
    Types typeUtils = environment.getTypeUtils();
    Elements elementUtils = environment.getElementUtils();
    TypeMirror superType = element.getSuperclass();
    String superTypeName = superType.toString();

    if (superTypeName.equals(Object.class.getCanonicalName())) {
      return;
    }

    TypeElement typeElement = elementUtils.getTypeElement(typeUtils.erasure(superType).toString());
    boolean isAbstract = typeElement.getModifiers().contains(Modifier.ABSTRACT);
    AutoForm autoForm = typeElement.getAnnotation(AutoForm.class);

    if (autoForm == null && !isAbstract) {
      throw new InvalidModelException("Class extends from '" + superType + "' that is not annotated with @" + AutoForm.class.getSimpleName() + ". Either annotate it or manually implement a form");
    }

    if (skipRoot != null && typeUtils.isSameType(typeElement.asType(), skipRoot.type)) {
      if (skipRoot.isClassLike()) {
        // Skip resolving the class as it is the same as skipRoot. This would cause an infinite resolution cycle
        // otherwise.
        merge(classModel, (ClassLikeModel) skipRoot);
      }
    } else {
      ClassLikeModel superTypeModel = getOrInspectClass(superType, typeElement, environment, classModel);
      merge(classModel, superTypeModel);
    }
  }

  /**
   * Merges the contents of {@code superClassModel} into {@code classModel}.
   *
   * @throws InvalidModelException if {@code classModel} contains the same field name as in {@code superClassModel}.
   */
  private void merge(ClassLikeModel classModel, ClassLikeModel superClassModel) {
    for (FieldModel field : classModel.getFields()) {
      FieldModel parentField = superClassModel.getFieldByPropertyName(field.propertyName());

      if (parentField != null && !parentField.isIgnored()) {
        throw new InvalidModelException("Class contains a field (" + field.getName()
            .toString() + ") with the same name as one in its superclass");
      }
    }

    classModel.merge(superClassModel);
  }

  /**
   * Validates that {@code rootElement} contains a zero-arg constructor.
   *
   * @throws InvalidModelException if the element does not contain a zero-arg constructor.
   */
  private void validateNoArgConstructor(Element rootElement) {
    ExecutableElement constructor = getNoArgConstructor(rootElement);

    if (constructor == null && !rootElement.getKind().equals(ElementKind.ENUM)) {
      throw new InvalidModelException("Class must contain a public constructor with no arguments");
    }
  }

  /**
   * Inspects {@code rootElement}'s and derives an {@code InterfaceModel}
   * <p>
   * This method triggers an inheritance resolution call.
   *
   * @param rootElement to be inspected.
   * @param environment the current processing environment.
   * @throws InvalidModelException if an invalid model has been derived anywhere from this element's type hierarchy.
   */
  private InterfaceModel inspectInterface(TypeElement rootElement, ProcessingEnvironment environment) {
    if (!rootElement.getKind().isInterface()) {
      throw new RuntimeException("Element is not an interface: " + rootElement);
    }

    AutoForm autoForm = rootElement.getAnnotation(AutoForm.class);
    if (autoForm == null) {
      // this is a bug
      throw new RuntimeException("Missing @AutoForm annotation on: " + rootElement);
    }

    AutoForm.Type[] subTypes = autoForm.subTypes();

    if (subTypes.length == 0) {
      throw new InvalidModelException("No subtypes provided");
    }

    Elements elementUtils = environment.getElementUtils();
    PackageElement declaredPackage = elementUtils.getPackageOf(rootElement);

    InterfaceModel interfaceModel = new InterfaceModel(rootElement, declaredPackage);
    List<Model> subTypeElements = inspectSubTypes(rootElement, interfaceModel, environment, Set.of(subTypes));
    interfaceModel.setSubTypes(subTypeElements);

    return interfaceModel;
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
