package ai.swim.structure.processor.recognizer;

import ai.swim.structure.processor.context.ScopedContext;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.List;

public class ListRecognizerModel extends StructuralRecognizer {
  private final RecognizerModel delegate;
  private final TypeMirror type;

  private ListRecognizerModel(TypeMirror type, RecognizerModel delegate) {
    this.type = type;
    this.delegate = delegate;
  }

  public static StructuralRecognizer from(TypeMirror typeMirror, ScopedContext context) {
    DeclaredType variableType = (DeclaredType) typeMirror;
    List<? extends TypeMirror> typeArguments = variableType.getTypeArguments();

    if (typeArguments.size() != 1) {
      throw new IllegalArgumentException("Attempted to build a list type that has more than one type parameter");
    }

    TypeMirror listType = typeArguments.get(0);

    switch (listType.getKind()) {
      case DECLARED:
        RecognizerModel delegate = RecognizerModel.from(listType, context);
        return new ListRecognizerModel(typeMirror, delegate);
      case TYPEVAR:
        return new ListRecognizerModel(typeMirror, RecognizerReference.untyped(listType));
      case WILDCARD:
        WildcardType wildcardType = (WildcardType) listType;
        TypeMirror bound = wildcardType.getExtendsBound();

        if (bound == null) {
          bound = wildcardType.getSuperBound();
        }

        ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
        Types typeUtils = processingEnvironment.getTypeUtils();
        Elements elementUtils = processingEnvironment.getElementUtils();

        TypeElement listTypeElement = elementUtils.getTypeElement(List.class.getCanonicalName());

        if (bound == null) {
          TypeElement objectTypeElement = elementUtils.getTypeElement(Object.class.getCanonicalName());
          DeclaredType declaredType = typeUtils.getDeclaredType(listTypeElement, objectTypeElement.asType());

          return new ListRecognizerModel(declaredType, RecognizerReference.untyped(objectTypeElement.asType()));
        } else {
          DeclaredType declaredType = typeUtils.getDeclaredType(listTypeElement, bound);
          return new ListRecognizerModel(declaredType, RecognizerModel.from(bound, context));
        }
      default:
        throw new AssertionError("ListRecognizer: " + listType.getKind());
    }
  }

  @Override
  public String recognizerInitializer() {
    return String.format("new ai.swim.structure.recognizer.std.collections.ListRecognizer<>(%s)", this.delegate.recognizerInitializer());
  }

  @Override
  public TypeMirror type() {
    return this.type;
  }

}
