package ai.swim.structure.processor.writer;

import ai.swim.structure.processor.context.ScopedContext;
import ai.swim.structure.processor.schema.ClassSchema;
import ai.swim.structure.processor.schema.FieldModel;
import com.squareup.javapoet.*;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static ai.swim.structure.processor.writer.Recognizer.TYPE_READ_EVENT;

public abstract class Builder {

  protected static final String RECOGNIZING_BUILDER_CLASS = "ai.swim.structure.RecognizingBuilder";
  protected static final String FIELD_RECOGNIZING_BUILDER_CLASS = "ai.swim.structure.FieldRecognizingBuilder";
  protected static final String RECOGNIZING_BUILDER_FEED_INDEX = "feedIndexed";
  protected static final String RECOGNIZING_BUILDER_BIND = "bind";
  protected static final String RECOGNIZING_BUILDER_RESET = "reset";

  protected final ClassSchema schema;
  protected final ScopedContext context;
  protected TypeName target;
  protected List<FieldModel> fields;

  public Builder(ClassSchema schema, ScopedContext context) {
    this.schema = schema;
    this.context = context;
  }

  public TypeSpec build(TypeName ty) {
    this.target = ty;
    this.fields = getFields();

    TypeSpec.Builder builder = init();

    builder.addSuperinterface(ty);
    builder.addFields(buildFields());
    builder.addMethods(Arrays.asList(buildMethods()));

    return builder.build();
  }

  protected List<FieldSpec> buildFields() {
    List<FieldSpec> fieldSpecs = new ArrayList<>(fields.size());

    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Elements elementUtils = processingEnvironment.getElementUtils();
    Types typeUtils = processingEnvironment.getTypeUtils();

    for (FieldModel recognizer : this.fields) {
      TypeElement fieldFieldRecognizingBuilder = elementUtils.getTypeElement(RECOGNIZING_BUILDER_CLASS);

      TypeMirror recognizerType = recognizer.type();

      if (recognizerType.getKind().isPrimitive()) {
        TypeElement boxedClass = typeUtils.boxedClass((PrimitiveType) recognizer.type());
        recognizerType = boxedClass.asType();
      }

      DeclaredType memberRecognizingBuilder = typeUtils.getDeclaredType(fieldFieldRecognizingBuilder, recognizerType);
      FieldSpec.Builder fieldSpec = FieldSpec.builder(TypeName.get(memberRecognizingBuilder), context.getNameFactory().fieldBuilderName(recognizer.fieldName()), Modifier.PRIVATE);

      fieldSpec.initializer(CodeBlock.of("new $L<>($L$L)", FIELD_RECOGNIZING_BUILDER_CLASS, recognizer.initializer(), recognizer.transformation()));

      fieldSpecs.add(fieldSpec.build());
    }

    return fieldSpecs;
  }

  private MethodSpec[] buildMethods() {
    return new MethodSpec[]{buildFeedIndexed(), buildBind(), buildReset()};
  }

  private MethodSpec buildReset() {
    MethodSpec.Builder builder = MethodSpec.methodBuilder(RECOGNIZING_BUILDER_RESET)
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .returns(this.target);
    builder.addCode(buildResetBlock());

    return builder.build();
  }

  protected MethodSpec buildBind() {
    MethodSpec.Builder builder = MethodSpec.methodBuilder(RECOGNIZING_BUILDER_BIND)
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .returns(TypeName.get(this.context.getRoot().asType()));
    builder.addCode(buildBindBlock());

    return builder.build();
  }

  private MethodSpec buildFeedIndexed() {
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Elements elementUtils = processingEnvironment.getElementUtils();
    TypeElement readEventType = elementUtils.getTypeElement(TYPE_READ_EVENT);
    MethodSpec.Builder builder = MethodSpec.methodBuilder(RECOGNIZING_BUILDER_FEED_INDEX)
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .addParameter(Integer.TYPE, "index")
        .addParameter(TypeName.get(readEventType.asType()), "event")
        .returns(boolean.class);

    builder.addCode(buildFeedIndexedBlock());

    return builder.build();
  }

  abstract TypeSpec.Builder init();

  abstract CodeBlock buildBindBlock();

  abstract CodeBlock buildFeedIndexedBlock();

  abstract CodeBlock buildResetBlock();

  abstract List<FieldModel> getFields();
}
