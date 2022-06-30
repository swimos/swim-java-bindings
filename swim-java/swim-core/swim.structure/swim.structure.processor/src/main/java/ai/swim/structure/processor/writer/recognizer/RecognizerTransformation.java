package ai.swim.structure.processor.writer.recognizer;

import ai.swim.structure.processor.context.ScopedContext;
import ai.swim.structure.processor.schema.FieldModel;
import ai.swim.structure.processor.writer.Emitter;
import com.squareup.javapoet.CodeBlock;

public class RecognizerTransformation implements Emitter {

  private final FieldModel fieldModel;

  public RecognizerTransformation(FieldModel fieldModel) {
    this.fieldModel = fieldModel;
  }

  @Override
  public CodeBlock emit(ScopedContext context) {
    switch (fieldModel.getFieldKind()) {
      case Body:
        return CodeBlock.of(".asBodyRecognizer()");
      case Attr:
        return CodeBlock.of(".asAttrRecognizer()");
      default:
        return CodeBlock.of("");
    }
  }

}
