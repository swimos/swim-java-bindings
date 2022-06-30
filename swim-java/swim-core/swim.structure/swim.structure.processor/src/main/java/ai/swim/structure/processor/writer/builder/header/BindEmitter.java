package ai.swim.structure.processor.writer.builder.header;

import ai.swim.structure.processor.context.ScopedContext;
import ai.swim.structure.processor.schema.ClassSchema;
import ai.swim.structure.processor.schema.FieldModel;
import ai.swim.structure.processor.writer.Emitter;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;

import java.util.List;

public class BindEmitter implements Emitter {
  private final List<FieldModel> fields;

  public BindEmitter(List<FieldModel> fields) {
    this.fields =fields;
  }

  @Override
  public CodeBlock emit(ScopedContext context) {
    ClassName classType = ClassName.bestGuess(context.getNameFactory().headerCanonicalName());

    CodeBlock.Builder body = CodeBlock.builder();
    body.add("$T obj = new $T();\n\n", classType, classType);

    for (FieldModel field : this.fields) {
      String builderName = context.getNameFactory().fieldBuilderName(field.fieldName());

      if (field.isOptional()) {
        body.addStatement("obj.$L = this.$L.bindOr($L)", field.fieldName(), builderName, field.defaultValue());
      } else {
        body.addStatement("obj.$L = this.$L.bind()", field.fieldName(), builderName);
      }
    }

    body.add("\nreturn obj;");

    return body.build();
  }
}
