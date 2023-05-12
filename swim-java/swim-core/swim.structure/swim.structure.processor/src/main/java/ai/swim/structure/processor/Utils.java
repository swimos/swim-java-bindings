package ai.swim.structure.processor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.function.Predicate;

public class Utils {

  public static <A extends Annotation> ExecutableElement accessorFor(List<ExecutableElement> methods, String prefix, Name name, Class<A> annotation, Predicate<A> predicate) {
    for (ExecutableElement method : methods) {
      String methodName = method.getSimpleName().toString().toLowerCase();
      String fieldName = name.toString().toLowerCase();

      if (methodName.contentEquals(prefix + fieldName)) {
        return method;
      }

      A actualAnnotation = method.getAnnotation(annotation);

      if (actualAnnotation != null) {
        if (predicate.test(actualAnnotation)) {
          return method;
        }
      }
    }

    return null;
  }

  public static <T> boolean isSubType(ProcessingEnvironment processingEnvironment, TypeMirror mirror, Class<T> target) {
    if (mirror.getKind() == TypeKind.DECLARED) {
      Elements elementUtils = processingEnvironment.getElementUtils();
      Types typeUtils = processingEnvironment.getTypeUtils();

      DeclaredType variableType = (DeclaredType) mirror;
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
