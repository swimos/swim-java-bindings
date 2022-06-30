package ai.swim.structure.processor.recognizer;

import ai.swim.structure.processor.Utils;
import ai.swim.structure.processor.context.ScopedContext;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.List;

import static ai.swim.structure.processor.Utils.unrollType;

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
      throw new IllegalArgumentException("Attempted to build a list from " + typeArguments.size() + " type parameters");
    }

    TypeMirror listType = typeArguments.get(0);
    Utils.UnrolledType unrolledType = unrollType(context, listType);

    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Types typeUtils = processingEnvironment.getTypeUtils();
    Elements elementUtils = processingEnvironment.getElementUtils();

    TypeElement listTypeElement = elementUtils.getTypeElement(List.class.getCanonicalName());
    DeclaredType typedList = typeUtils.getDeclaredType(listTypeElement, unrolledType.typeMirror);

    return new ListRecognizerModel(typedList, unrolledType.recognizerModel);
  }

  @Override
  public String recognizerInitializer() {
    return String.format("new ai.swim.structure.recognizer.std.collections.ListRecognizer<>(%s)", this.delegate.recognizerInitializer());
  }

  @Override
  public TypeMirror type(ProcessingEnvironment environment) {
    return this.type;
  }

  @Override
  public RecognizerModel retyped(ScopedContext context) {
    return new ListRecognizerModel(this.type,this.delegate.retyped(context));
  }

}
