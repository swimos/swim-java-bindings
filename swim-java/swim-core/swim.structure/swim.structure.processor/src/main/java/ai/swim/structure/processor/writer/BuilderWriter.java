package ai.swim.structure.processor.writer;

import ai.swim.structure.processor.context.ScopedContext;
import ai.swim.structure.processor.schema.ClassSchema;
import ai.swim.structure.processor.schema.FieldModel;
import ai.swim.structure.processor.schema.HeaderFields;
import ai.swim.structure.processor.schema.PartitionedFields;
import com.squareup.javapoet.*;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static ai.swim.structure.processor.writer.RecognizerWriter.*;

public class BuilderWriter {
  private static final String FIELD_RECOGNIZING_BUILDER_CLASS = "ai.swim.structure.FieldRecognizingBuilder";
  public static final String RECOGNIZING_BUILDER_FEED_INDEX = "feedIndexed";
  public static final String RECOGNIZING_BUILDER_BIND = "bind";
  public static final String RECOGNIZING_BUILDER_RESET = "reset";

  public static void writeHeader(TypeSpec.Builder parentSpec, ClassSchema schema, ScopedContext context) throws IOException {
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Types typeUtils = processingEnvironment.getTypeUtils();
    Elements elementUtils = processingEnvironment.getElementUtils();

    TypeSpec.Builder classSpec = TypeSpec.classBuilder(context.getFormatter().headerBuilderClassName())
        .addModifiers(Modifier.PRIVATE, Modifier.FINAL);

    TypeElement recognizingBuilderElement = elementUtils.getTypeElement(RECOGNIZING_BUILDER_CLASS);
    DeclaredType recognizingBuilderType = typeUtils.getDeclaredType(recognizingBuilderElement, context.getRoot().asType());

    classSpec.addSuperinterface(TypeName.get(recognizingBuilderType));
    classSpec.addMethod(buildConstructor());
    classSpec.addFields(Arrays.asList(buildFields(schema, context)));
    classSpec.addMethods(Arrays.asList(buildMethods(schema, context)));

    parentSpec.addType(classSpec.build());
  }

  public static void writeInto(TypeSpec.Builder parentSpec, ClassSchema schema, ScopedContext context) {
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Types typeUtils = processingEnvironment.getTypeUtils();
    Elements elementUtils = processingEnvironment.getElementUtils();

    TypeSpec.Builder classSpec = TypeSpec.classBuilder(context.getFormatter().builderClassName())
        .addModifiers(Modifier.PRIVATE, Modifier.FINAL);

    TypeElement recognizingBuilderElement = elementUtils.getTypeElement(RECOGNIZING_BUILDER_CLASS);
    DeclaredType recognizingBuilderType = typeUtils.getDeclaredType(recognizingBuilderElement, context.getRoot().asType());

    classSpec.addSuperinterface(TypeName.get(recognizingBuilderType));
    classSpec.addMethod(buildConstructor());
    classSpec.addFields(Arrays.asList(buildFields(schema, context)));
    classSpec.addMethods(Arrays.asList(buildMethods(schema, context)));

    parentSpec.addType(classSpec.build());
  }

  private static MethodSpec[] buildMethods(ClassSchema schema, ScopedContext context) {
    MethodSpec[] methods = new MethodSpec[3];
    methods[0] = buildFeedIndexed(schema, context);
    methods[1] = buildBind(schema, context);
    methods[2] = buildReset(schema, context);

    return methods;
  }

  private static MethodSpec buildFeedIndexed(ClassSchema schema, ScopedContext context) {
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Elements elementUtils = processingEnvironment.getElementUtils();
    TypeElement readEventType = elementUtils.getTypeElement(TYPE_READ_EVENT);
    MethodSpec.Builder builder = MethodSpec.methodBuilder(RECOGNIZING_BUILDER_FEED_INDEX)
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .addParameter(Integer.TYPE, "index")
        .addParameter(TypeName.get(readEventType.asType()), "event")
        .returns(boolean.class);

    CodeBlock.Builder body = CodeBlock.builder();
    body.beginControlFlow("switch (index)");

    List<FieldModel> recognizers = schema.getPartitionedFields().flatten();

    for (int i = 0; i < recognizers.size(); i++) {
      FieldModel recognizer = recognizers.get(i);
      body.add("case $L:", i);
      body.addStatement("\nreturn this.$L.feed(event)", context.getFormatter().fieldBuilderName(recognizer.fieldName()));
    }

    body.add("default:").addStatement("\nthrow new RuntimeException(\"Unknown idx: \" + index)").endControlFlow();
    builder.addCode(body.build());

    return builder.build();
  }

  private static MethodSpec buildBind(ClassSchema schema, ScopedContext context) {
    MethodSpec.Builder builder = MethodSpec.methodBuilder(RECOGNIZING_BUILDER_BIND)
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .returns(TypeName.get(context.getRoot().asType()));

    CodeBlock.Builder body = CodeBlock.builder();
    body.add("$T obj = new $T();\n\n", context.getRoot().asType(), context.getRoot().asType());

    for (FieldModel recognizer : schema.getPartitionedFields().flatten()) {
      String fieldName = context.getFormatter().fieldBuilderName(recognizer.fieldName());

      if (recognizer.isOptional()) {
        recognizer.getAccessor().write(body, "obj", String.format("this.%s.bindOr(%s)", fieldName, recognizer.defaultValue()));
      } else {
        recognizer.getAccessor().write(body, "obj", String.format("this.%s.bind()", fieldName));
      }
    }

    body.add("\nreturn obj;");
    builder.addCode(body.build());

    return builder.build();
  }

  private static MethodSpec buildReset(ClassSchema schema, ScopedContext context) {
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Elements elementUtils = processingEnvironment.getElementUtils();
    Types typeUtils = processingEnvironment.getTypeUtils();

    TypeElement recognizingBuilderElement = elementUtils.getTypeElement(RECOGNIZING_BUILDER_CLASS);
    DeclaredType recognizingBuilderType = typeUtils.getDeclaredType(recognizingBuilderElement, context.getRoot().asType());

    MethodSpec.Builder builder = MethodSpec.methodBuilder(RECOGNIZING_BUILDER_RESET)
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .returns(TypeName.get(recognizingBuilderType));

    CodeBlock.Builder body = CodeBlock.builder();

    for (FieldModel recognizer : schema.getPartitionedFields().flatten()) {
      String fieldName = context.getFormatter().fieldBuilderName(recognizer.fieldName());
      ;
      body.addStatement("this.$L = this.$L.reset()", fieldName, fieldName);
    }

    body.addStatement("return this");
    builder.addCode(body.build());

    return builder.build();
  }

  private static MethodSpec buildConstructor() {
    return MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC).build();
  }

  private static FieldSpec[] buildFields(ClassSchema schema, ScopedContext context) {
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Elements elementUtils = processingEnvironment.getElementUtils();
    Types typeUtils = processingEnvironment.getTypeUtils();
    TypeElement fieldFieldRecognizingBuilder = elementUtils.getTypeElement(RECOGNIZING_BUILDER_CLASS);

    List<FieldModel> recognizers = schema.getPartitionedFields().flatten();
    int recognizerCount = recognizers.size();
    FieldSpec[] fields = new FieldSpec[recognizerCount];

    for (int i = 0; i < recognizerCount; i++) {
      FieldModel recognizer = recognizers.get(i);
      TypeMirror recognizerType = recognizer.type();

      if (recognizerType.getKind().isPrimitive()) {
        TypeElement boxedClass = typeUtils.boxedClass((PrimitiveType) recognizer.type());
        recognizerType = boxedClass.asType();
      }

      DeclaredType memberRecognizingBuilder = typeUtils.getDeclaredType(fieldFieldRecognizingBuilder, recognizerType);
      FieldSpec.Builder fieldSpec = FieldSpec.builder(TypeName.get(memberRecognizingBuilder), context.getFormatter().fieldBuilderName(recognizer.fieldName()), Modifier.PRIVATE);

      fieldSpec.initializer(CodeBlock.of("new $L<>($L$L)", FIELD_RECOGNIZING_BUILDER_CLASS, recognizer.initializer(), recognizer.transformation()));

      fields[i] = fieldSpec.build();
    }

    return fields;
  }

  private static CodeBlock buildHeaderIndexFn(ClassSchema schema, ScopedContext context) {
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Elements elementUtils = processingEnvironment.getElementUtils();

    PartitionedFields partitionedFields = schema.getPartitionedFields();
    HeaderFields headerFieldSet = partitionedFields.headerFields;

    int idx = 0;
    CodeBlock.Builder body = CodeBlock.builder();
    body.add("(key) -> {\n");

    if (headerFieldSet.hasTagName()) {
      idx += 1;
      body.beginControlFlow("if (key.isTag())");
      body.addStatement("return 0");
      body.endControlFlow();
    }

    TypeElement headerSlotKey = elementUtils.getTypeElement(DELEGATE_HEADER_SLOT_KEY);

    body.beginControlFlow("if (key.isHeaderSlot())");
    body.addStatement("$T headerSlotKey = ($T) key", headerSlotKey, headerSlotKey);

    WriterUtils.writeIndexSwitchBlock(
        body,
        "switch (headerSlotKey.getName())",
        idx,
        (offset, i) -> {
          if (i == headerFieldSet.headerFields.size()) {
            return null;
          } else {
            FieldModel recognizer = headerFieldSet.headerFields.get(i - offset);
            return String.format("case \"%s:\r\n\t return %s", recognizer.propertyName(), i);
          }
        }
    );

    body.endControlFlow();
    body.addStatement("return null");
    body.add("}");

    return body.build();
  }

}
