package ai.swim.structure.processor;

import ai.swim.structure.annotations.AutoForm;
import ai.swim.structure.processor.structure.ConstructorElement;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ElementInspector {

  private final Map<String, ElementMap> cache;

  public ElementInspector() {
    cache = new HashMap<>();
  }

  public ElementMap getOrInspect(Element element, ProcessingEnvironment environment) {
    ElementMap map = this.cache.get(element.toString());

    if (map != null) {
      return map;
    }

    return inspectAndInsertClass(element, environment);
  }

  private ElementMap inspectAndInsertClass(Element element, ProcessingEnvironment environment) {
    Messager messager = environment.getMessager();
    ConstructorElement constructor = getConstructor(element, messager);

    if (constructor == null) {
      return null;
    }

    ElementMap elementMap = new ElementMap(element, constructor);


    if (!inspectClass(element, elementMap)) {
      return null;
    }

    if (!inspectSuperclasses(element, elementMap, environment)) {
      return null;
    }

    System.out.println(elementMap);

    this.cache.put(element.toString(), elementMap);
    return elementMap;
  }

  private boolean inspectClass(Element rootElement, ElementMap elementMap) {
    for (Element element : rootElement.getEnclosedElements()) {
      switch (element.getKind()) {
        case FIELD:
          elementMap.addField(FieldView.from((VariableElement) element));
          break;
        case METHOD:
          elementMap.addMethod((ExecutableElement) element);
          break;
      }
    }

    return true;
  }

  private boolean inspectSuperclasses(Element element, ElementMap elementMap, ProcessingEnvironment environment) {
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

      ElementMap superTypeMap = getOrInspect(typeElement, environment);
      if (superTypeMap == null) {
        return false;
      }

      if (!validateAndMerge(elementMap, superTypeMap, messager)) {
        return false;
      }
    }

    return true;
  }

  private static boolean validateAndMerge(ElementMap elementMap, ElementMap superTypeMap, Messager messager) {
    for (FieldView field : elementMap.getFields()) {
      if (superTypeMap.getField(field.getName().toString()) != null) {
        messager.printMessage(Diagnostic.Kind.ERROR, "Class '" + elementMap.getRoot() + "' contains a field (" + field.getName().toString() + ") with the same name as one in its superclass");
        return false;
      }
    }

    elementMap.merge(superTypeMap);

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
