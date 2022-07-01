package ai.swim.structure.processor.writer.builder.header;

import ai.swim.structure.processor.context.ScopedContext;
import ai.swim.structure.processor.schema.ClassSchema;
import ai.swim.structure.processor.schema.FieldDiscriminate;
import ai.swim.structure.processor.schema.FieldModel;
import ai.swim.structure.processor.writer.Emitter;
import ai.swim.structure.processor.writer.builder.Builder;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static ai.swim.structure.processor.writer.Lookups.RECOGNIZING_BUILDER_BIND;

public class HeaderBuilder extends Builder {
  public HeaderBuilder(ClassSchema classSchema, ScopedContext context) {
    super(classSchema, context);
  }

  @Override
  protected TypeSpec.Builder init() {
    return TypeSpec.classBuilder(context.getNameFactory().headerBuilderClassName())
        .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL);
  }

  @Override
  protected MethodSpec buildBind() {
    ClassName classType = ClassName.bestGuess(context.getNameFactory().headerCanonicalName());
    MethodSpec.Builder builder = MethodSpec.methodBuilder(RECOGNIZING_BUILDER_BIND)
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .returns(classType);
    builder.addCode(buildBindBlock().emit(context));

    return builder.build();
  }

  @Override
  protected Emitter buildBindBlock() {
    return new BindEmitter(fields);
  }

  @Override
  protected Emitter buildFeedIndexedBlock() {
    return new FeedIndexedEmitther(fields);
  }

  @Override
  protected Emitter buildResetBlock() {
    return new ResetEmitter(fields);
  }

  @Override
  protected List<FieldModel> getFields() {
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
