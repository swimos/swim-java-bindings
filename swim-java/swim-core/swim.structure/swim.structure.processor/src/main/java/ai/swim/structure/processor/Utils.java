package ai.swim.structure.processor;

import ai.swim.structure.annotations.AutoForm;
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
import java.util.List;

public class Utils {

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
        TypeMirror bound = wildcardType.getExtendsBound();

        if (bound == null) {
          bound = wildcardType.getSuperBound();
        } else {
          TypeMirror superBound = wildcardType.getSuperBound();
          if (superBound != null) {
            String message = "cannot derive a generic field that contains both a super & extends bound";
            context.getMessager().error(message);
            throw new RuntimeException(message);
          }
        }

        ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
        Elements elementUtils = processingEnvironment.getElementUtils();

        if (bound == null) {
          TypeElement objectTypeElement = elementUtils.getTypeElement(Object.class.getCanonicalName());
          return new UnrolledType(objectTypeElement.asType(), modelLookup.untyped(objectTypeElement.asType()));
        } else {
          return new UnrolledType(bound, modelLookup.lookup(bound, context));
        }
      default:
        throw new AssertionError("Unrolled type: " + typeMirror.getKind());
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
