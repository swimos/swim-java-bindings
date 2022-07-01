package ai.swim.structure.processor.writer.builder.classBuilder;

import ai.swim.structure.processor.context.NameFactory;
import ai.swim.structure.processor.context.ScopedContext;
import ai.swim.structure.processor.schema.ClassSchema;
import ai.swim.structure.processor.schema.FieldDiscriminate;
import ai.swim.structure.processor.writer.Emitter;
import com.squareup.javapoet.CodeBlock;

public class ResetEmitter implements Emitter {
  private final ClassSchema schema;

  public ResetEmitter(ClassSchema schema) {
    this.schema = schema;
  }

  @Override
  public CodeBlock emit(ScopedContext context) {
    CodeBlock.Builder body = CodeBlock.builder();
    NameFactory nameFactory = context.getNameFactory();

    for (FieldDiscriminate field : schema.discriminate()) {
      String fieldName;
      if (field.isHeader()) {
        fieldName = nameFactory.headerBuilderFieldName();
      } else {
        FieldDiscriminate.SingleField fieldDiscriminate = (FieldDiscriminate.SingleField) field;
        fieldName = context.getNameFactory().fieldBuilderName(fieldDiscriminate.getField().fieldName());
      }

      body.addStatement("this.$L = this.$L.reset()", fieldName, fieldName);
    }

    body.addStatement("return this");
    return body.build();
  }
}
