package ai.swim.structure.processor;

import ai.swim.structure.annotations.AutoForm;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.List;

public class ElementUtils {

  public static ExecutableElement setterFor(List<ExecutableElement> methods, Name name) {
    for (ExecutableElement method : methods) {
      String methodName = method.getSimpleName().toString().toLowerCase();
      String fieldName = name.toString().toLowerCase();

      if (methodName.contentEquals("set" + fieldName)) {
        return method;
      }

      AutoForm.Setter setter = method.getAnnotation(AutoForm.Setter.class);

      if (setter != null) {
        if (setter.value().equals(name.toString())) {
          return method;
        }
      }
    }

    return null;
  }

  public static <T> boolean isSubType(ProcessingEnvironment processingEnvironment, Element element, Class<T> target) {
    if (element.asType().getKind() == TypeKind.DECLARED) {
      Elements elementUtils = processingEnvironment.getElementUtils();
      Types typeUtils = processingEnvironment.getTypeUtils();

      DeclaredType variableType = (DeclaredType) element.asType();
      TypeMirror[] targetGenerics = variableType.getTypeArguments().toArray(TypeMirror[]::new);

      if (targetGenerics.length != target.getTypeParameters().length) {
        return false;
      }

      TypeElement targetElement = elementUtils.getTypeElement(target.getCanonicalName());
      DeclaredType targetDeclaredType = typeUtils.getDeclaredType(targetElement, targetGenerics);

      return typeUtils.isSubtype(variableType, targetDeclaredType);
    } else {
      return false;
    }
  }

  public static ExecutableElement getNoArgConstructor(Element rootElement) {
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
          return constructor;
        }
      }
    }

    return null;
  }

}
