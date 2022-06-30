package ai.swim.structure.processor.writer.builder;

import ai.swim.structure.processor.context.ScopedContext;
import ai.swim.structure.processor.schema.FieldModel;
import ai.swim.structure.processor.writer.Emitter;
import ai.swim.structure.processor.writer.recognizer.RecognizerTransformation;
import com.squareup.javapoet.CodeBlock;

import static ai.swim.structure.processor.writer.Lookups.FIELD_RECOGNIZING_BUILDER_CLASS;

public class FieldInitializer implements Emitter {

  private final FieldModel fieldModel;

  public FieldInitializer(FieldModel fieldModel) {
    this.fieldModel = fieldModel;
  }

  @Override
  public CodeBlock emit(ScopedContext context) {
    return CodeBlock.of("new $L<>($L$L)", FIELD_RECOGNIZING_BUILDER_CLASS, fieldModel.initializer(), new RecognizerTransformation(fieldModel).emit(context));
  }
}
