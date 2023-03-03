package ai.swim.structure.processor;

import ai.swim.structure.processor.context.ScopedContext;
import ai.swim.structure.processor.models.Model;
import ai.swim.structure.processor.models.ModelLookup;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.WildcardType;
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

  public static UnrolledType unrollType(ModelLookup modelLookup, ScopedContext context, TypeMirror typeMirror) {
    switch (typeMirror.getKind()) {
      case DECLARED:
        return new UnrolledType(typeMirror, modelLookup.lookup(typeMirror, context));
      case TYPEVAR:
        return new UnrolledType(typeMirror, modelLookup.untyped(typeMirror));
      case WILDCARD:
        WildcardType wildcardType = (WildcardType) typeMirror;
        return unrollBoundedType(wildcardType.getExtendsBound(), wildcardType.getSuperBound(), modelLookup, context);
      default:
        throw new AssertionError("Unrolled type: " + typeMirror.getKind());
    }
  }

  public static UnrolledType unrollBoundedType(TypeMirror lowerBound, TypeMirror upperBound, ModelLookup modelLookup, ScopedContext context) {
    TypeMirror bound = lowerBound;

    if (bound == null || bound.getKind() == TypeKind.NULL) {
      bound = upperBound;
    } else if (upperBound != null && upperBound.getKind() != TypeKind.NULL) {
      String message = "cannot derive a generic field that contains both a lower & upper bound";
      context.getMessager().error(message);
      throw new RuntimeException(message);
    }

    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Elements elementUtils = processingEnvironment.getElementUtils();

    if (bound == null) {
      TypeElement objectTypeElement = elementUtils.getTypeElement(Object.class.getCanonicalName());
      return new UnrolledType(objectTypeElement.asType(), modelLookup.untyped(objectTypeElement.asType()));
    } else {
      // we need to retype the model here so that the new, unrolled, type is shifted up a level. I.e, if the type that
      // we're unrolling is List<? extends Number> then the new model is List<Number> and that is now the element's
      // type; this will then be propagated up to the callee when they retype the field itself.
      Model unrolled = modelLookup.lookup(bound, context);
      return new UnrolledType(unrolled.type(processingEnvironment), unrolled);
    }
  }

  public static class UnrolledType {
    public TypeMirror typeMirror;
    public Model model;

    public UnrolledType(TypeMirror typeMirror, Model model) {
      this.typeMirror = typeMirror;
      this.model = model;
    }
  }

}
