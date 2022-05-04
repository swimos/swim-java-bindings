package ai.swim.structure.processor;

import ai.swim.structure.annotations.AutoForm;
import ai.swim.structure.processor.structure.ConstructorElement;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ElementInspector {

  private final Map<String, ClassMap> cache;

  public ElementInspector() {
    cache = new HashMap<>();
  }

  public ClassMap getOrInspect(Element element, ProcessingEnvironment environment) {
    ClassMap map = this.cache.get(element.toString());

    if (map != null) {
      return map;
    }

    return inspectAndInsertClass(element, environment);
  }

  private ClassMap inspectAndInsertClass(Element element, ProcessingEnvironment environment) {
    Messager messager = environment.getMessager();
    ConstructorElement constructor = getConstructor(element, messager);

    if (constructor == null) {
      return null;
    }

    ClassMap classMap = new ClassMap(element, constructor);


    if (!inspectClass(element, classMap)) {
      return null;
    }

    if (!inspectSuperclasses(element, classMap, environment)) {
      return null;
    }

    if (!inspectGenerics(element, environment)) {
      return null;
    }

    this.cache.put(element.toString(), classMap);
    return classMap;
  }

  private boolean inspectGenerics(Element element,  ProcessingEnvironment environment) {
    DeclaredType declaredType = (DeclaredType) element.asType();

    if (declaredType.getTypeArguments().size() != 0) {
      environment.getMessager().printMessage(Diagnostic.Kind.ERROR, "Class: '" + element + "' has generic type arguments which are not yet supported");
      return false;
    }

    return true;
  }

  private boolean inspectClass(Element rootElement, ClassMap classMap) {
    for (Element element : rootElement.getEnclosedElements()) {
      switch (element.getKind()) {
        case FIELD:
          classMap.addField(FieldView.from((VariableElement) element));
          break;
        case METHOD:
          classMap.addMethod((ExecutableElement) element);
          break;
      }
    }

    return true;
  }

  private boolean inspectSuperclasses(Element element, ClassMap classMap, ProcessingEnvironment environment) {
    Types typeUtils = environment.getTypeUtils();
    Elements elementUtils = environment.getElementUtils();
    Messager messager = environment.getMessager();
    List<? extends TypeMirror> superTypes = typeUtils.directSupertypes(element.asType());

    for (TypeMirror superType : superTypes) {
      if (superType.toString().equals(Object.class.getCanonicalName())) {
        continue;
      }

      TypeElement typeElement = elementUtils.getTypeElement(superType.toString());

      AutoForm autoForm = typeElement.getAnnotation(AutoForm.class);
      if (autoForm == null) {
        messager.printMessage(Diagnostic.Kind.ERROR, "Class '" + element + "' extends from '" + superType + "' that is not" + " annotated with @" + AutoForm.class.getSimpleName() + ". Either annotate it or manually implement a form");
        return false;
      }

      ClassMap superTypeMap = getOrInspect(typeElement, environment);
      if (superTypeMap == null) {
        return false;
      }

      if (!validateAndMerge(classMap, superTypeMap, messager)) {
        return false;
      }
    }

    return true;
  }

  private static boolean validateAndMerge(ClassMap classMap, ClassMap superTypeMap, Messager messager) {
    for (FieldView field : classMap.getFields()) {
      if (superTypeMap.getField(field.getName().toString()) != null) {
        messager.printMessage(Diagnostic.Kind.ERROR, "Class '" + classMap.getRoot() + "' contains a field (" + field.getName().toString() + ") with the same name as one in its superclass");
        return false;
      }
    }

    classMap.merge(superTypeMap);

    return true;
  }

  private ConstructorElement getConstructor(Element rootElement, Messager messager) {
    for (Element element : rootElement.getEnclosedElements()) {
      ElementKind elementKind = element.getKind();

      if (elementKind == ElementKind.CONSTRUCTOR) {
        ExecutableElement constructor = (ExecutableElement) element;
        boolean isPublic = false;

        for (Modifier modifier : constructor.getModifiers()) {
          if (modifier == Modifier.PUBLIC) {
            isPublic = true;
            break;
          }
        }

        if (!isPublic) {
          continue;
        }

        if (constructor.getParameters().size() == 0) {
          return new ConstructorElement(constructor);
        }
      }
    }

    messager.printMessage(Diagnostic.Kind.ERROR, "Class '" + rootElement + "' must contain a public constructor with no arguments");
    return null;
  }

}
