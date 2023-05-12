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
import ai.swim.structure.processor.model.mapping.KnownTypeModel;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.*;

import static ai.swim.structure.processor.Utils.accessorFor;
import static ai.swim.structure.processor.Utils.getNoArgConstructor;

public class ModelInspector {
  private final HashMap<TypeMirror, Model> models;

  public ModelInspector() {
    models = new HashMap<>();
  }

  public Model getOrInspect(TypeElement element, ProcessingEnvironment environment) {
    return getOrInspect(element.asType(), element, environment, null);
  }

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

  private ClassLikeModel getOrInspectClass(TypeMirror type, TypeElement element, ProcessingEnvironment environment, StructuralModel skip) {
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

  private Model resolveDeclaredType(TypeMirror type, TypeElement element, ProcessingEnvironment environment, StructuralModel current) {
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

  private Model inspectDeclaredType(TypeMirror type, TypeElement element, ProcessingEnvironment environment, StructuralModel current) {
    Model resolved = resolveDeclaredType(type, element, environment, current);
    if (resolved == null) {
      Elements elementUtils = environment.getElementUtils();
      PackageElement packageElement = elementUtils.getPackageOf(element);
      return new UnresolvedModel(element.asType(), element, packageElement);
    } else {
      return resolved;
    }
  }

  private Model inspectElement(TypeMirror elementType, Element element, ProcessingEnvironment environment, StructuralModel skipRoot) {
    ElementKind kind = element.getKind();
    Model model = KnownTypeModel.getLibraryModel(environment, this, element, elementType);

    if (model != null) {
      return model;
    } else if (kind.isClass() || kind.isInterface()) {
      return inspectDeclaredType(elementType, (TypeElement) element, environment, skipRoot);
    } else if (kind.isField()) {
      VariableElement variableElement = (VariableElement) element;
      TypeMirror fieldType = variableElement.asType();

      TypeKind typeKind = fieldType.getKind();
      model = KnownTypeModel.getLibraryModel(environment, this, element, variableElement.asType());

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
        return new UnresolvedModel(fieldType, element, packageElement);
      }
    }

    throw new AssertionError("Unsupported type: " + kind);
  }

  private ClassLikeModel inspectClass(TypeMirror type, TypeElement element, ProcessingEnvironment environment, StructuralModel skipSubType) {
    validateNoArgConstructor(element);

    Elements elementUtils = environment.getElementUtils();
    PackageElement declaredPackage = elementUtils.getPackageOf(element);

    ClassLikeModel classModel = new ClassLikeModel(type, element, declaredPackage);

    inspectTag(element);
    inspectClass(classModel, element, environment);

    if (!element.getKind().equals(ElementKind.ENUM)) {
      inspectSuperclass(element, classModel, environment, skipSubType);
    }

    inspectClassSubTypes(element, classModel, environment);

    return classModel;
  }

  private void inspectTag(TypeElement element) {
    if (element.getKind().equals(ElementKind.ENUM)) {
      if (element.getAnnotation(AutoForm.Tag.class) != null) {
        throw new InvalidModelException(String.format("%s cannot be used on enumerations, only on constants", AutoForm.Tag.class.getCanonicalName()));
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
        classModel.addField(new FieldModel(new FieldAccessor(variableName.toString()), fieldModel, variable, fieldKind));
      } else {
        ExecutableElement setter = accessorFor(classModel.getMethods(), "set", variableName, AutoForm.Setter.class, (anno) -> anno.value().equals(variableName.toString()));
        ExecutableElement getter = accessorFor(classModel.getMethods(), "get", variableName, AutoForm.Getter.class, (anno) -> anno.value().equals(variableName.toString()));

        if (rootElement.getKind() != ElementKind.ENUM) {
          validateSetter(setter, variableName, variableType, environment);
        }

        validateGetter(getter, variableName, variableType, environment);
        classModel.addField(new FieldModel(new MethodAccessor(setter, getter), fieldModel, variable, fieldKind));
      }
    }
  }

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

  private List<Model> inspectSubTypes(Element rootElement, StructuralModel model, ProcessingEnvironment environment, Set<AutoForm.Type> subTypes) {
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

      if (rootElement.getKind() == ElementKind.CLASS && !typeUtils.directSupertypes(subTypeMirror).contains(rootType)) {
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

  private void validateSetter(ExecutableElement setter, Name name, TypeMirror expectedType, ProcessingEnvironment environment) {
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

  private void validateGetter(ExecutableElement getter, Name name, TypeMirror expectedType, ProcessingEnvironment environment) {
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

  private FieldKind getFieldKind(Element element) {
    AutoForm.Kind kind = element.getAnnotation(AutoForm.Kind.class);
    if (kind == null) {
      return FieldKind.Slot;
    } else {
      return kind.value();
    }
  }

  private void inspectSuperclass(TypeElement element, ClassLikeModel classModel, ProcessingEnvironment environment, StructuralModel skipRoot) {
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
        merge(classModel, (ClassLikeModel) skipRoot);
      }
    } else {
      ClassLikeModel superTypeModel = getOrInspectClass(superType, typeElement, environment, classModel);
      merge(classModel, superTypeModel);
    }
  }

  private void merge(ClassLikeModel classModel, ClassLikeModel superClassModel) {
    for (FieldModel field : classModel.getFields()) {
      FieldModel parentField = superClassModel.getFieldByPropertyName(field.propertyName());

      if (parentField != null && !parentField.isIgnored()) {
        throw new InvalidModelException("Class contains a field (" + field.getName().toString() + ") with the same name as one in its superclass");
      }
    }

    classModel.merge(superClassModel);
  }

  private void validateNoArgConstructor(Element rootElement) {
    ExecutableElement constructor = getNoArgConstructor(rootElement);

    if (constructor == null && !rootElement.getKind().equals(ElementKind.ENUM)) {
      throw new InvalidModelException("Class must contain a public constructor with no arguments");
    }
  }

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

}
