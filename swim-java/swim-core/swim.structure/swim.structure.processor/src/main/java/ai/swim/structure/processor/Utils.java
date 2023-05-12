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

  /**
   * Searches for an accessor (getter or setter) in a provided list of methods that matches either a predicate test or
   * that the method's name is prefixed by 'prefix' and suffixed by 'name', while ignoring casing.
   *
   * @param methods    to search through.
   * @param prefix     that the method name must start by.
   * @param name       that the method name must end by.
   * @param annotation that the method may be annotated with. This annotation is passed to the predicate to test.
   * @param predicate  to test for method name validation.
   * @param <A>        the type of the annotation.
   * @return a matching method or null if one was not found.
   */
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

  /**
   * Asserts whether a {@link TypeMirror} is a subtype of a class.
   *
   * @param processingEnvironment the current processing environment.
   * @param mirror                to check against {@code target}.
   * @param target                to check against {@code mirror}.
   * @param <T>                   the type of the class.
   * @return whether {@code mirror} is a subtype of {@code target}.
   */
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

  /**
   * Searches in {@code rootElement} for a root-level, public, zero-arg constructor.
   *
   * @param rootElement to search within.
   * @return a matching constructor or null if one was not found.
   */
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
