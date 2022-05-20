package ai.swim.structure.processor.writer;

import ai.swim.structure.processor.context.NameFactory;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static ai.swim.structure.processor.writer.RecognizerWriter.*;

public class BuilderWriter {
  private static final String FIELD_RECOGNIZING_BUILDER_CLASS = "ai.swim.structure.FieldRecognizingBuilder";
  public static final String RECOGNIZING_BUILDER_FEED_INDEX = "feedIndexed";
  public static final String RECOGNIZING_BUILDER_BIND = "bind";
  public static final String RECOGNIZING_BUILDER_RESET = "reset";

  public static void writeHeaderBuilder(TypeSpec.Builder parentSpec, ClassSchema schema, ScopedContext context) {
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Elements elementUtils = processingEnvironment.getElementUtils();
    NameFactory nameFactory = context.getNameFactory();

    TypeSpec.Builder classSpec = TypeSpec.classBuilder(context.getNameFactory().headerBuilderClassName())
        .addModifiers(Modifier.PRIVATE, Modifier.FINAL);

    ClassName classType = ClassName.bestGuess(nameFactory.headerCanonicalName());
    TypeElement recognizingBuilderElement = elementUtils.getTypeElement(RECOGNIZING_BUILDER_CLASS);
    ParameterizedTypeName builderType = ParameterizedTypeName.get(ClassName.get(recognizingBuilderElement), classType);

    classSpec.addSuperinterface(builderType);

    PartitionedFields partitionedFields = schema.getPartitionedFields();
    List<FieldModel> fieldModels = new ArrayList<>(partitionedFields.headerFields.headerFields);

    FieldModel tagBody = partitionedFields.headerFields.tagBody;

    if (tagBody != null) {
      fieldModels.add(tagBody);
    }

    classSpec.addFields(buildFields(fieldModels, context));
    classSpec.addMethods(Arrays.asList(buildMethods(schema, context, classType)));

    parentSpec.addType(classSpec.build());


    TypeSpec.Builder headerClass = TypeSpec.classBuilder(context.getNameFactory().headerClassName()).addModifiers(Modifier.PRIVATE, Modifier.FINAL);
    for (FieldModel headerField : partitionedFields.headerFields.headerFields) {
      headerClass.addField(FieldSpec.builder(TypeName.get(headerField.type()), headerField.fieldName()).build());
    }

    parentSpec.addType(headerClass.build());
  }

  public static void writeBuilder(TypeSpec.Builder parentSpec, ClassSchema schema, ScopedContext context) {
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Types typeUtils = processingEnvironment.getTypeUtils();
    Elements elementUtils = processingEnvironment.getElementUtils();

    TypeSpec.Builder classSpec = TypeSpec.classBuilder(context.getNameFactory().builderClassName())
        .addModifiers(Modifier.PRIVATE, Modifier.FINAL);

    TypeElement recognizingBuilderElement = elementUtils.getTypeElement(RECOGNIZING_BUILDER_CLASS);
    DeclaredType recognizingBuilderType = typeUtils.getDeclaredType(recognizingBuilderElement, context.getRoot().asType());

    classSpec.addSuperinterface(TypeName.get(recognizingBuilderType));

    PartitionedFields partitionedFields = schema.getPartitionedFields();

    List<FieldModel> fields = new ArrayList<>();
    fields.addAll(partitionedFields.headerFields.attributes);
    fields.addAll(partitionedFields.body.getFields());

    List<FieldSpec> fieldSpecs = buildFields(fields, context);

    if (partitionedFields.hasHeaderFields()) {
      fieldSpecs.add(0, buildHeaderField(schema, context));
    }

    TypeName classType = ClassName.get(context.getRoot().asType());

    classSpec.addFields(fieldSpecs);
    classSpec.addMethods(Arrays.asList(buildMethods(schema, context, classType)));

    parentSpec.addType(classSpec.build());

    if (!schema.getPartitionedFields().headerFields.headerFields.isEmpty()) {
      writeHeaderBuilder(parentSpec, schema, context);
    }
  }

  private static FieldSpec buildHeaderField(ClassSchema schema, ScopedContext context) {
    NameFactory nameFactory = context.getNameFactory();
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Elements elementUtils = processingEnvironment.getElementUtils();

    TypeElement recognizingBuilderElement = elementUtils.getTypeElement(RECOGNIZING_BUILDER_CLASS);
    ParameterizedTypeName builderType = ParameterizedTypeName.get(ClassName.get(recognizingBuilderElement), ClassName.bestGuess(nameFactory.headerCanonicalName()));

    FieldSpec.Builder fieldBuilder = FieldSpec.builder(
        builderType,
        nameFactory.headerBuilderFieldName(),
        Modifier.PRIVATE
    );

    PartitionedFields partitionedFields = schema.getPartitionedFields();
    int numSlots = partitionedFields.headerFields.headerFields.size();

    boolean hasBody = partitionedFields.headerFields.hasTagBody();
    CodeBlock indexFn = buildHeaderIndexFn(schema, context);
    String headerBuilder = nameFactory.headerBuilderCanonicalName();

    CodeBlock.Builder initializer = CodeBlock.builder();
    initializer.add(
        "$L($L, () -> new $L(), $L, $L)",
        nameFactory.headerBuilderMethod(),
        hasBody,
        headerBuilder,
        numSlots,
        indexFn
    );

    return fieldBuilder.initializer(initializer.build()).build();
  }

  private static MethodSpec[] buildMethods(ClassSchema schema, ScopedContext context, TypeName classType) {
    MethodSpec[] methods = new MethodSpec[3];
    methods[0] = buildFeedIndexed(schema, context);
    methods[1] = buildBind(schema, context, classType);
    methods[2] = buildReset(schema, context, classType);

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

    NameFactory nameFactory = context.getNameFactory();
    List<FieldDiscriminate> discriminate = schema.discriminate();

    for (int i = 0; i < discriminate.size(); i++) {
      FieldDiscriminate field = discriminate.get(i);
      String fieldName;

      if (field.isHeader()) {
        fieldName = nameFactory.headerBuilderFieldName();
      } else {
        fieldName = nameFactory.fieldBuilderName(((FieldDiscriminate.SingleField) field).getField().fieldName());
      }

      body.add("case $L:", i);
      body.addStatement("\nreturn this.$L.feed(event)", fieldName);
    }

    body.add("default:").addStatement("\nthrow new RuntimeException(\"Unknown idx: \" + index)").endControlFlow();
    builder.addCode(body.build());

    return builder.build();
  }

  private static MethodSpec buildBind(ClassSchema schema, ScopedContext context, TypeName classType) {
    MethodSpec.Builder builder = MethodSpec.methodBuilder(RECOGNIZING_BUILDER_BIND)
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .returns(classType);

    NameFactory nameFactory = context.getNameFactory();
    CodeBlock.Builder body = CodeBlock.builder();
    body.add("$T obj = new $T();\n\n", classType, classType);

    for (FieldDiscriminate fieldDiscriminate : schema.discriminate()) {
      if (fieldDiscriminate.isHeader()) {
        FieldDiscriminate.HeaderFields headerFields = (FieldDiscriminate.HeaderFields) fieldDiscriminate;
        Elements elementUtils = context.getProcessingEnvironment().getElementUtils();
        TypeElement headerElement = elementUtils.getTypeElement(nameFactory.headerBuilderClassName());

        body.addStatement("$T header = this.headerBuilder.bind()", headerElement);

        for (FieldModel field : headerFields.getFields()) {
          body.addStatement("obj.$L = header.$L", field.fieldName());
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
    builder.addCode(body.build());

    return builder.build();
  }

  private static MethodSpec buildReset(ClassSchema schema, ScopedContext context, TypeName classType) {
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Elements elementUtils = processingEnvironment.getElementUtils();

    TypeElement recognizingBuilderElement = elementUtils.getTypeElement(RECOGNIZING_BUILDER_CLASS);
    ParameterizedTypeName builderType = ParameterizedTypeName.get(ClassName.get(recognizingBuilderElement), classType);

    MethodSpec.Builder builder = MethodSpec.methodBuilder(RECOGNIZING_BUILDER_RESET)
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .returns(builderType);

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
    builder.addCode(body.build());

    return builder.build();
  }

  private static List<FieldSpec> buildFields(List<FieldModel> fields, ScopedContext context) {
    List<FieldSpec> fieldSpecs = new ArrayList<>(fields.size());

    for (FieldModel field : fields) {
      fieldSpecs.add(buildField(context, field));
    }

    return fieldSpecs;
  }

  private static FieldSpec buildField(ScopedContext context, FieldModel recognizer) {
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Elements elementUtils = processingEnvironment.getElementUtils();
    Types typeUtils = processingEnvironment.getTypeUtils();
    TypeElement fieldFieldRecognizingBuilder = elementUtils.getTypeElement(RECOGNIZING_BUILDER_CLASS);

    TypeMirror recognizerType = recognizer.type();

    if (recognizerType.getKind().isPrimitive()) {
      TypeElement boxedClass = typeUtils.boxedClass((PrimitiveType) recognizer.type());
      recognizerType = boxedClass.asType();
    }

    DeclaredType memberRecognizingBuilder = typeUtils.getDeclaredType(fieldFieldRecognizingBuilder, recognizerType);
    FieldSpec.Builder fieldSpec = FieldSpec.builder(TypeName.get(memberRecognizingBuilder), context.getNameFactory().fieldBuilderName(recognizer.fieldName()), Modifier.PRIVATE);

    fieldSpec.initializer(CodeBlock.of("new $L<>($L$L)", FIELD_RECOGNIZING_BUILDER_CLASS, recognizer.initializer(), recognizer.transformation()));

    return fieldSpec.build();
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
        "headerSlotKey.getName()",
        idx,
        (offset, i) -> {
          if (i == headerFieldSet.headerFields.size()) {
            return null;
          } else {
            FieldModel recognizer = headerFieldSet.headerFields.get(i - offset);
            return String.format("case \"%s\":\r\n\t return %s;\r\n", recognizer.propertyName(), i);
          }
        }
    );

    body.endControlFlow();
    body.addStatement("return null");
    body.add("}");

    return body.build();
  }

}
