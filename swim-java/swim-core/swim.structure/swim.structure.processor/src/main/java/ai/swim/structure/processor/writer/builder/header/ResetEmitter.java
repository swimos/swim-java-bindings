package ai.swim.structure.processor.writer.builder.header;

import ai.swim.structure.processor.context.ScopedContext;
import ai.swim.structure.processor.schema.FieldModel;
import ai.swim.structure.processor.writer.Emitter;
import com.squareup.javapoet.CodeBlock;

import java.util.List;

public class ResetEmitter implements Emitter {
  private final List<FieldModel> fields;

  public ResetEmitter(List<FieldModel> fields) {this.fields = fields;
  }

  @Override
  public CodeBlock emit(ScopedContext context) {
    CodeBlock.Builder body = CodeBlock.builder();

    for (FieldModel field : this.fields) {
      String fieldName = context.getNameFactory().fieldBuilderName(field.fieldName());
      body.addStatement("this.$L = this.$L.reset()", fieldName, fieldName);
    }

    body.addStatement("return this");

    return body.build();
  }
}
