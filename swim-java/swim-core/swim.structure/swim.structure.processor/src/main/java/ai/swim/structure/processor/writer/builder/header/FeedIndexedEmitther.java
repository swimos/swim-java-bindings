package ai.swim.structure.processor.writer.builder.header;

import ai.swim.structure.processor.context.ScopedContext;
import ai.swim.structure.processor.schema.FieldModel;
import ai.swim.structure.processor.writer.Emitter;
import com.squareup.javapoet.CodeBlock;

import java.util.List;

public class FeedIndexedEmitther implements Emitter {
  private final List<FieldModel> fields;

  public FeedIndexedEmitther(List<FieldModel> fields) {
    this.fields = fields;
  }

  @Override
  public CodeBlock emit(ScopedContext context) {
    CodeBlock.Builder body = CodeBlock.builder();

    body.beginControlFlow("switch (index)");

    for (int i = 0; i < this.fields.size(); i++) {
      FieldModel field = fields.get(i);
      String fieldName = context.getNameFactory().fieldBuilderName(field.fieldName());

      body.add("case $L:", i);
      body.addStatement("\nreturn this.$L.feed(event)", fieldName);
    }

    body.add("default:").addStatement("\nthrow new RuntimeException(\"Unknown idx: \" + index)").endControlFlow();

    return body.build();
  }
}
