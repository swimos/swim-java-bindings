package ai.swim.structure.processor;

import ai.swim.structure.annotations.AutoForm;
import ai.swim.structure.processor.context.ProcessingContext;
import ai.swim.structure.processor.structure.ConstructorElement;

import javax.lang.model.element.*;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.List;

public class ElementMap {
  private final ConstructorElement constructor;
  private final List<FieldView> memberVariables;
  private final List<ExecutableElement> methods;

  public ElementMap(ConstructorElement constructor, List<FieldView> memberVariables, List<ExecutableElement> methods) {
    this.constructor = constructor;
    this.memberVariables = memberVariables;
    this.methods = methods;
  }

  public FieldView getVariable(String name) {
    for (FieldView memberVariable : this.memberVariables) {
      if (memberVariable.getName().contentEquals(name)) {
        return memberVariable;
      }
    }

    return null;
  }

  public ExecutableElement getMethod(String name) {
    for (ExecutableElement method : this.methods) {
      if (method.getSimpleName().contentEquals(name)) {
        return method;
      }
    }

    return null;
  }

  public List<ExecutableElement> getMethods() {
    return methods;
  }

  public List<FieldView> getFields() {
    return memberVariables;
  }

  public static <E extends Element> ElementMap from(List<E> elements, ProcessingContext context) {
    ConstructorElement constructorElement = null;
    List<FieldView> memberVariables = new ArrayList<>();
    List<ExecutableElement> methods = new ArrayList<>();

    for (Element element : elements) {
      ElementKind elementKind = element.getKind();

      if (!elementKind.isClass()) {
        // We want to skip nested classes as if they are annotated then they will be picked up by the processor.

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
            constructorElement = new ConstructorElement(constructor);
          }
        } else if (elementKind == ElementKind.FIELD) {
          memberVariables.add(FieldView.from((VariableElement) element));
        } else if (elementKind == ElementKind.METHOD) {
          methods.add((ExecutableElement) element);
        } else {
          throw new AssertionError("Unimplemented partition type for: " + element + ", kind: " + elementKind);
        }
      }
    }

    if (constructorElement == null) {
      context.getProcessingEnvironment().getMessager().printMessage(Diagnostic.Kind.ERROR, "Class must contain a public constructor with no arguments");
      return null;
    }

    return new ElementMap(constructorElement, memberVariables, methods);
  }

  public ExecutableElement setterFor(Name name) {
    for (ExecutableElement method : this.methods) {
      if (method.getSimpleName().toString().toLowerCase().contentEquals("set"+name)) {
        return method;
      }

      AutoForm.Setter setter = method.getAnnotation(AutoForm.Setter.class);

      if (setter != null) {
        if (name.toString().equals(name.toString())) {
          return method;
        }
      }
    }

    return null;
  }

  public ConstructorElement getConstructor() {
    return this.constructor;
  }
}
