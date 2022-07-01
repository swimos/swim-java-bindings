package ai.swim.structure.processor.recognizer;

import ai.swim.structure.processor.context.ScopedContext;
import com.squareup.javapoet.CodeBlock;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.List;

public class RecognizerInstance extends RecognizerModel {
  private final String init;

  public RecognizerInstance(TypeMirror type, String init) {
    super(type, ModelKind.Reference);
    this.init = init;
  }

  public RecognizerInstance(TypeMirror typeMirror, ModelKind kind, String init) {
    super(typeMirror, kind);
    this.init = init;
  }

  @Override
  public CodeBlock initializer(ScopedContext context, boolean inConstructor) {
    return CodeBlock.of("$L", init);
  }

  public static Resolver resolver(String root) {
    return new Resolver(root);
  }

  public static class Resolver {
    private final String root;

    Resolver(String root) {
      this.root = root;
    }

    public RecognizerInstance resolve(ProcessingEnvironment environment, String elementName) {
      Elements elementUtils = environment.getElementUtils();
      Types typeUtils = environment.getTypeUtils();
      TypeElement element = elementUtils.getTypeElement(String.format("%s.%s", root, elementName));

      if (element == null) {
        element = elementUtils.getTypeElement(root);
        DeclaredType declaredType = (DeclaredType) element.asType();

        for (Element enclosedElement : declaredType.asElement().getEnclosedElements()) {
          if (enclosedElement.getKind().isField()) {
            VariableElement field = (VariableElement) enclosedElement;

            if (field.getSimpleName().contentEquals(elementName)) {
              List<? extends TypeMirror> recognizerArgument = declaredType.getTypeArguments();

              if (field.asType().getKind() == TypeKind.DECLARED) {
                DeclaredType fieldType = (DeclaredType) field.asType();
                List<? extends TypeMirror> typeArguments = fieldType.getTypeArguments();

                if (typeArguments.size() == 0) {
                  return new RecognizerInstance(field.asType(), String.format("%s.%s", root, elementName));
                } else if (typeArguments.size() == 1) {
                  return new RecognizerInstance(typeArguments.get(0), String.format("%s.%s", root, elementName));
                } else {
                  throw new RuntimeException("Recognizer has more than one type parameter: " + elementName);
                }
              } else {
                return new RecognizerInstance(field.asType(), String.format("%s.%s", root, elementName));
              }
            }
          }
        }

        throw new IllegalStateException(String.format("%s.%s could not be resolved", root, elementName));
      } else {
        return new RecognizerInstance(typeUtils.erasure(element.asType()), String.format("%s.%s", root, elementName));
      }
    }
  }
}
