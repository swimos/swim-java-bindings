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

package ai.swim.structure.processor.inspect;

import ai.swim.structure.annotations.AutoForm;
import ai.swim.structure.annotations.FieldKind;
import ai.swim.structure.processor.context.ScopedContext;
import ai.swim.structure.processor.context.ScopedMessager;
import ai.swim.structure.processor.inspect.accessor.FieldAccessor;
import ai.swim.structure.processor.inspect.accessor.MethodAccessor;
import ai.swim.structure.processor.inspect.elements.ClassElement;
import ai.swim.structure.processor.inspect.elements.FieldElement;
import ai.swim.structure.processor.inspect.elements.InterfaceElement;
import ai.swim.structure.processor.inspect.elements.StructuralElement;
import ai.swim.structure.processor.inspect.elements.UnresolvedElement;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static ai.swim.structure.processor.Utils.accessorFor;
import static ai.swim.structure.processor.Utils.getNoArgConstructor;

public abstract class ElementInspector {

  private ElementInspector() {

  }

  public static StructuralElement inspect(TypeElement element, ScopedContext context) {
    ElementKind kind = element.getKind();
    if (kind.isClass()) {
      return inspectClass(element, context);
    } else if (kind.isInterface()) {
      return inspectInterface(element, context);
    } else {
      throw new AssertionError("Unsupported structure type for inspection: " + kind);
    }
  }

  private static ClassElement inspectClass(TypeElement element, ScopedContext context) {
    ProcessingEnvironment env = context.getProcessingEnvironment();
    ConstructorElement constructor = getConstructor(element, context.getMessager());

    if (constructor == null) {
      return null;
    }

    Elements elementUtils = env.getElementUtils();
    PackageElement declaredPackage = elementUtils.getPackageOf(element);

    ClassElement classElement = new ClassElement(element, declaredPackage);

    if (!inspectTag(element, context)) {
      return null;
    }

    if (!inspectClass(element, classElement, context)) {
      return null;
    }

    if (!element.getKind().equals(ElementKind.ENUM) && !inspectSuperclasses(element, classElement, context)) {
      return null;
    }

    return classElement;
  }

  private static boolean inspectTag(TypeElement element, ScopedContext context) {
    ScopedMessager messager = context.getMessager();

    if (element.getKind().equals(ElementKind.ENUM)) {
      if (element.getAnnotation(AutoForm.Tag.class)!=null){
        messager.error(String.format("%s cannot be used on enumerations, only on constants", AutoForm.Tag.class.getCanonicalName()));
        return false;
      }

      List<? extends Element> enclosedElements = element.getEnclosedElements();
      HashSet<String> tags = new HashSet<>();

      for (Element enclosedElement : enclosedElements) {
        if (enclosedElement.getKind().equals(ElementKind.ENUM_CONSTANT)) {
          AutoForm.Tag currentTag = enclosedElement.getAnnotation(AutoForm.Tag.class);
          String constantTag;
          if (currentTag != null && !currentTag.value().isBlank()) {
            if (!currentTag.value().chars().allMatch(Character::isLetterOrDigit)) {
              messager.error(String.format("invalid characters in tag: '%s'",  currentTag.value()));
              return false;
            }

            constantTag = currentTag.value();
          } else {
            constantTag = enclosedElement.toString();
          }

          if (!tags.add(constantTag)) {
            messager.error(String.format("contains a duplicate tag: '%s'", constantTag));
            return false;
          }
        }
      }
    } else {
      AutoForm.Tag tag = element.getAnnotation(AutoForm.Tag.class);

      if (tag != null && !tag.value().isBlank()) {
        return tag.value().chars().anyMatch(Character::isLetterOrDigit);
      }
    }

    return true;
  }

  private static boolean inspectClass(Element rootElement, ClassElement classElement, ScopedContext ctx) {
    Types typeUtils = ctx.getProcessingEnvironment().getTypeUtils();
    List<VariableElement> variables = new ArrayList<>();

    for (Element element : rootElement.getEnclosedElements()) {
      switch (element.getKind()) {
        case FIELD:
          variables.add((VariableElement) element);
          break;
        case METHOD:
          classElement.addMethod((ExecutableElement) element);
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
        ctx.getMessager().error("recursive types are not supported by auto forms");
        return false;
      }

      if (!manifest.validate(fieldKind, ctx.getMessager())) {
        return false;
      }

      if (isPublic) {
        classElement.addField(new FieldElement(new FieldAccessor(variableName.toString()), variable, fieldKind));
      } else {
        ExecutableElement setter = accessorFor(classElement.getMethods(), "set", variableName, AutoForm.Setter.class, (anno) -> anno.value().equals(variableName.toString()));
        ExecutableElement getter = accessorFor(classElement.getMethods(), "get", variableName, AutoForm.Getter.class, (anno) -> anno.value().equals(variableName.toString()));

        if (rootElement.getKind() != ElementKind.ENUM && !validateSetter(setter, variableName, variableType, ctx)) {
          return false;
        }

        if (!validateGetter(getter, variableName, variableType, ctx)) {
          return false;
        }

        classElement.addField(new FieldElement(new MethodAccessor(setter, getter), variable, fieldKind));
      }
    }

    return inspectClassSubTypes(rootElement, classElement, ctx);
  }

  private static boolean inspectClassSubTypes(Element rootElement, ClassElement classElement, ScopedContext ctx) {
    boolean isAbstract = rootElement.getModifiers().stream().anyMatch(modifier -> modifier.equals(Modifier.ABSTRACT));
    ScopedMessager messager = ctx.getMessager();

    AutoForm autoForm = rootElement.getAnnotation(AutoForm.class);
    if (autoForm == null) {
      // This is fine as it's a class that has an abstract supertype that we can't deserialize directly into.
      return true;
    }

    AutoForm.Type[] subTypes = autoForm.subTypes();

    if (!isAbstract && subTypes.length == 0) {
      return true;
    } else if (isAbstract && subTypes.length == 0) {
      messager.error("Class missing subtypes");
      return false;
    }

    List<StructuralElement> subTypeElements = inspectSubTypes(rootElement, ctx, subTypes);
    if (subTypeElements == null) {
      return false;
    }

    classElement.setAbstract(isAbstract);
    classElement.setSubTypes(subTypeElements);

    return true;
  }

  private static List<StructuralElement> inspectSubTypes(Element rootElement, ScopedContext ctx, AutoForm.Type[] subTypes) {
    ProcessingEnvironment processingEnvironment = ctx.getProcessingEnvironment();
    Types typeUtils = processingEnvironment.getTypeUtils();
    Elements elementUtils = processingEnvironment.getElementUtils();
    ScopedMessager messager = ctx.getMessager();

    List<StructuralElement> subTypeElements = new ArrayList<>(subTypes.length);
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
        messager.error("Subtype cannot be root type");
        return null;
      }

      if (!typeUtils.isSubtype(typeUtils.erasure(subTypeMirror), typeUtils.erasure(rootType))) {
        messager.error(String.format("%s is not a subtype of %s", subTypeMirror, rootType));
        return null;
      }

      PackageElement declaredPackage = elementUtils.getPackageOf(subTypeElement);
      subTypeElements.add(new UnresolvedElement((TypeElement) subTypeElement, declaredPackage));
    }

    return subTypeElements;
  }

  private static boolean validateSetter(ExecutableElement setter, Name name, TypeMirror expectedType, ScopedContext ctx) {
    ScopedMessager messager = ctx.getMessager();
    Types typeUtils = ctx.getProcessingEnvironment().getTypeUtils();

    if (setter == null) {
      messager.error("Private field: '" + name + "' has no setter");
      return false;
    }

    if (!setter.getModifiers().contains(Modifier.PUBLIC)) {
      messager.error("Private field: '" + name + "' has no public setter");
      return false;
    }

    List<? extends VariableElement> parameters = setter.getParameters();
    if (parameters.size() != 1) {
      messager.error("expected a setter for field '" + name + "' that takes one parameter");
      return false;
    }

    VariableElement variableElement = parameters.get(0);

    if (!typeUtils.isSameType(variableElement.asType(), expectedType)) {
      String cause = String.format("Expected type: '%s', found: '%s'", variableElement.asType(), expectedType);
      messager.error("setter for field '" + name + "' accepts an incorrect type. Cause: " + cause);
      return false;
    }

    return true;
  }

  private static boolean validateGetter(ExecutableElement getter, Name name, TypeMirror expectedType, ScopedContext ctx) {
    ScopedMessager messager = ctx.getMessager();
    Types typeUtils = ctx.getProcessingEnvironment().getTypeUtils();

    if (getter == null) {
      messager.error("Private field: '" + name + "' has no getter");
      return false;
    }

    if (!getter.getModifiers().contains(Modifier.PUBLIC)) {
      messager.error("Private field: '" + name + "' has no public getter");
      return false;
    }

    if (!getter.getParameters().isEmpty()) {
      messager.error("getter for field '" + name + "' should have no parameter");
      return false;
    }

    if (!typeUtils.isSameType(getter.getReturnType(), expectedType)) {
      messager.error("getter for field '" + name + "' returns an incorrect type");
      return false;
    }

    return true;
  }


  private static FieldKind getFieldKind(Element element) {
    AutoForm.Kind kind = element.getAnnotation(AutoForm.Kind.class);
    if (kind == null) {
      return FieldKind.Slot;
    } else {
      return kind.value();
    }
  }

  private static boolean inspectSuperclasses(Element element, ClassElement classElement, ScopedContext context) {
    ProcessingEnvironment environment = context.getProcessingEnvironment();
    Types typeUtils = environment.getTypeUtils();
    Elements elementUtils = environment.getElementUtils();
    ScopedMessager messager = context.getMessager();

    List<? extends TypeMirror> superTypes = typeUtils.directSupertypes(element.asType());

    for (TypeMirror superType : superTypes) {
      if (superType.toString().equals(Object.class.getCanonicalName())) {
        continue;
      }

      TypeElement typeElement = elementUtils.getTypeElement(typeUtils.erasure(superType).toString());

      boolean isAbstract = typeElement.getModifiers().contains(Modifier.ABSTRACT);
      AutoForm autoForm = typeElement.getAnnotation(AutoForm.class);

      if (autoForm == null && !isAbstract) {
        messager.error("Class extends from '" + superType + "' that is not annotated with @" + AutoForm.class.getSimpleName() + ". Either annotate it or manually implement a form");
        return false;
      }

      if (!typeElement.getKind().isClass()) {
        continue;
      }

      ClassElement superTypeModel = inspectClass(typeElement, context);

      if (superTypeModel == null) {
        return false;
      }

      if (!validateAndMerge(classElement, superTypeModel, messager)) {
        return false;
      }
    }

    return true;
  }

  private static boolean validateAndMerge(ClassElement classElement, ClassElement superTypeElement, ScopedMessager messager) {
    for (FieldElement field : classElement.getFields()) {
      FieldElement parentField = superTypeElement.getFieldViewByPropertyName(field.propertyName());

      if (parentField != null && !parentField.isIgnored()) {
        messager.error("Class contains a field (" + field.getName().toString() + ") with the same name as one in its superclass");
        return false;
      }
    }

    classElement.merge(superTypeElement);

    return true;
  }

  private static ConstructorElement getConstructor(Element rootElement, ScopedMessager messager) {
    ExecutableElement constructor = getNoArgConstructor(rootElement);

    if (constructor == null && !rootElement.getKind().equals(ElementKind.ENUM)) {
      messager.error("Class must contain a public constructor with no arguments");
      return null;
    }

    return new ConstructorElement(constructor);
  }

  private static InterfaceElement inspectInterface(TypeElement rootElement, ScopedContext context) {
    if (!rootElement.getKind().isInterface()) {
      throw new RuntimeException("Element is not an interface: " + rootElement);
    }

    ScopedMessager messager = context.getMessager();

    AutoForm autoForm = rootElement.getAnnotation(AutoForm.class);
    if (autoForm == null) {
      // this is a bug
      throw new RuntimeException("Missing @AutoForm annotation on: " + rootElement);
    }

    AutoForm.Type[] subTypes = autoForm.subTypes();

    if (subTypes.length == 0) {
      messager.error("no subtypes provided");
      return null;
    }

    List<StructuralElement> subTypeElements = inspectSubTypes(rootElement, context, subTypes);
    if (subTypeElements == null) {
      return null;
    }

    ProcessingEnvironment env = context.getProcessingEnvironment();
    Elements elementUtils = env.getElementUtils();
    PackageElement declaredPackage = elementUtils.getPackageOf(rootElement);

    return new InterfaceElement(rootElement, declaredPackage, subTypeElements);
  }

}
