package ai.swim.structure.processor.writer;

import ai.swim.structure.processor.context.ScopedContext;
import ai.swim.structure.processor.schema.ClassSchema;
import ai.swim.structure.processor.schema.FieldModel;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class HeaderBuilder extends Builder {
  public HeaderBuilder(ClassSchema classSchema, ScopedContext context) {
    super(classSchema, context);
  }

  @Override
  TypeSpec.Builder init() {
    return TypeSpec.classBuilder(context.getNameFactory().headerBuilderClassName())
        .addModifiers(Modifier.PRIVATE, Modifier.STATIC,Modifier.FINAL);
  }

  @Override
  protected MethodSpec buildBind() {
    ClassName classType = ClassName.bestGuess(context.getNameFactory().headerCanonicalName());
    MethodSpec.Builder builder = MethodSpec.methodBuilder(RECOGNIZING_BUILDER_BIND)
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .returns(classType);
    builder.addCode(buildBindBlock());

    return builder.build();
  }

  @Override
  CodeBlock buildBindBlock() {
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

  @Override
  CodeBlock buildFeedIndexedBlock() {
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

  @Override
  CodeBlock buildResetBlock() {
    CodeBlock.Builder body = CodeBlock.builder();

    for (FieldModel field : this.fields) {
      String fieldName = context.getNameFactory().fieldBuilderName(field.fieldName());
      body.addStatement("this.$L = this.$L.reset()", fieldName, fieldName);
    }

    body.addStatement("return this");

    return body.build();
  }

  @Override
  List<FieldModel> getFields() {
    return schema.discriminate()
        .stream()
        .filter(FieldDiscriminate::isHeader)
        .flatMap(f -> {
          FieldDiscriminate.HeaderFields headerFields = (FieldDiscriminate.HeaderFields) f;
          FieldModel tagBody = headerFields.getTagBody();

          if (tagBody != null) {
            List<FieldModel> fields = new ArrayList<>(Collections.singleton(headerFields.getTagBody()));
            fields.addAll(headerFields.getFields());
            return fields.stream();
          } else {
            return headerFields.getFields().stream();
          }
        })
        .collect(Collectors.toList());
  }
}
