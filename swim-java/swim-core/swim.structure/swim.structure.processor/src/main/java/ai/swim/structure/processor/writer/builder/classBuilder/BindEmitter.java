package ai.swim.structure.processor.writer.builder.classBuilder;

import ai.swim.structure.processor.context.ScopedContext;
import ai.swim.structure.processor.schema.ClassSchema;
import ai.swim.structure.processor.schema.FieldDiscriminate;
import ai.swim.structure.processor.schema.FieldModel;
import ai.swim.structure.processor.writer.Emitter;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;

import javax.lang.model.type.TypeMirror;

public class BindEmitter implements Emitter {
  private final ClassSchema schema;

  public BindEmitter(ClassSchema schema) {
    this.schema = schema;
  }

  @Override
  public CodeBlock emit(ScopedContext context) {
    CodeBlock.Builder body = CodeBlock.builder();
    TypeMirror ty = context.getRoot().asType();
    body.add("$T obj = new $T();\n\n", ty, ty);

    for (FieldDiscriminate fieldDiscriminate : schema.discriminate()) {
      if (fieldDiscriminate.isHeader()) {
        FieldDiscriminate.HeaderFields headerFields = (FieldDiscriminate.HeaderFields) fieldDiscriminate;
        ClassName headerElement = ClassName.bestGuess(context.getNameFactory().headerCanonicalName());

        body.addStatement("$T header = this.headerBuilder.bind()", headerElement);

        for (FieldModel field : headerFields.getFields()) {
          field.getAccessor().write(body, "obj", String.format("header.%s", field.fieldName()));
        }

        FieldModel tagBody = headerFields.getTagBody();

        if (tagBody != null) {
          tagBody.getAccessor().write(body, "obj", String.format("header.%s", tagBody.fieldName()));
        }
      } else {
        FieldDiscriminate.SingleField singleField = (FieldDiscriminate.SingleField) fieldDiscriminate;
        FieldModel field = singleField.getField();
        String fieldName = context.getNameFactory().fieldBuilderName(field.fieldName());

        if (field.isOptional()) {
          field.getAccessor().write(body, "obj", String.format("this.%s.bindOr(%s)", fieldName, field.defaultValue()));
        } else {
          field.getAccessor().write(body, "obj", String.format("this.%s.bind()", fieldName));
        }
      }
    }

    body.add("\nreturn obj;");
    return body.build();
  }
}
