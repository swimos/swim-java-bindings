package ai.swim.structure.processor.writer.builder.classBuilder;

import ai.swim.structure.processor.context.ScopedContext;
import ai.swim.structure.processor.recognizer.RecognizerModel;
import ai.swim.structure.processor.writer.Emitter;
import com.squareup.javapoet.CodeBlock;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import static ai.swim.structure.processor.writer.Lookups.TYPE_PARAMETER;

public class TypeParameterEmitter implements Emitter {
  private final RecognizerModel model;

  public TypeParameterEmitter(RecognizerModel model) {
    this.model = model;
  }

  @Override
  public CodeBlock emit(ScopedContext context) {
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Elements elementUtils = processingEnvironment.getElementUtils();
    Types typeUtils = processingEnvironment.getTypeUtils();

    TypeElement typeElement = elementUtils.getTypeElement(TYPE_PARAMETER);
    TypeMirror erased = typeUtils.erasure(typeElement.asType());

    return CodeBlock.of("$T.from(() -> $L)", erased, model.initializer(context, false));
  }
}
