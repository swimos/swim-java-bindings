package ai.swim.structure.processor.writer;

import ai.swim.structure.processor.context.NameFactory;
import ai.swim.structure.processor.context.ScopedContext;
import ai.swim.structure.processor.schema.ClassSchema;
import ai.swim.structure.processor.schema.FieldModel;
import ai.swim.structure.processor.schema.HeaderSet;
import ai.swim.structure.processor.schema.PartitionedFields;
import com.squareup.javapoet.*;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ai.swim.structure.processor.writer.Recognizer.DELEGATE_HEADER_SLOT_KEY;
import static ai.swim.structure.processor.writer.Recognizer.RECOGNIZER_CLASS;
import static ai.swim.structure.processor.writer.WriterUtils.typeParametersToTypeVariable;

public class ClassBuilder extends Builder {

  public ClassBuilder(ClassSchema classSchema, ScopedContext context) {
    super(classSchema, context);
  }

  @Override
  TypeSpec.Builder init() {
    TypeSpec.Builder builder = TypeSpec.classBuilder(context.getNameFactory().builderClassName())
        .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
        .addMethod(buildDefaultConstructor())
        .addTypeVariables(typeParametersToTypeVariable(schema.getTypeParameters()));

    if (!schema.getTypeParameters().isEmpty()) {
      builder.addMethod(buildParameterisedConstructor());
    }

    return builder;
  }

  private MethodSpec buildDefaultConstructor() {
    return MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC).build();
  }

  private MethodSpec buildParameterisedConstructor() {
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Types typeUtils = processingEnvironment.getTypeUtils();
    Elements elementUtils = processingEnvironment.getElementUtils();
    NameFactory nameFactory = context.getNameFactory();

    TypeElement recognizerClass = elementUtils.getTypeElement(RECOGNIZER_CLASS);
    TypeElement fieldRecognizingBuilder = elementUtils.getTypeElement(FIELD_RECOGNIZING_BUILDER_CLASS);

    MethodSpec.Builder builder = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);
    List<ParameterSpec> parameters = new ArrayList<>();
    CodeBlock.Builder body = CodeBlock.builder();

    for (FieldModel fieldModel : schema.getClassMap().getFieldModels()) {
      if (fieldModel.getFieldView().isParameterised()) {
        String fieldBuilderName = nameFactory.fieldBuilderName(fieldModel.fieldName());
        DeclaredType typedRecognizer = typeUtils.getDeclaredType(recognizerClass, fieldModel.type());
        DeclaredType typedBuilder = typeUtils.getDeclaredType(fieldRecognizingBuilder, fieldModel.type());

        parameters.add(ParameterSpec.builder(TypeName.get(typedRecognizer), fieldModel.fieldName()).build());
        body.addStatement("this.$L = new $T($L)", fieldBuilderName, typedBuilder, fieldModel.fieldName());
      }
    }

    return builder.addParameters(parameters).addCode(body.build()).build();
  }

  @Override
  CodeBlock buildBindBlock() {
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

  @Override
  protected List<FieldSpec> buildFields() {
    List<FieldSpec> fieldSpecs = super.buildFields();

    if (schema.getPartitionedFields().hasHeaderFields()) {
      fieldSpecs.add(0, buildHeaderField());
    }

    return fieldSpecs;
  }

  private FieldSpec buildHeaderField() {
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
    int numSlots = partitionedFields.headerSet.headerFields.size();

    boolean hasBody = partitionedFields.headerSet.hasTagBody();
    CodeBlock indexFn = buildHeaderIndexFn();
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

  private CodeBlock buildHeaderIndexFn() {
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Elements elementUtils = processingEnvironment.getElementUtils();

    PartitionedFields partitionedFields = schema.getPartitionedFields();
    HeaderSet headerFieldSet = partitionedFields.headerSet;

    int idx = 0;
    CodeBlock.Builder body = CodeBlock.builder();
    body.add("(key) -> {\n");

    if (headerFieldSet.hasTagBody()) {
      body.beginControlFlow("if (key.isHeaderBody())");
      body.addStatement("return $L", idx);
      body.endControlFlow();

      idx += 1;
    }

    TypeElement headerSlotKey = elementUtils.getTypeElement(DELEGATE_HEADER_SLOT_KEY);

    body.beginControlFlow("if (key.isHeaderSlot())");
    body.addStatement("$T headerSlotKey = ($T) key", headerSlotKey, headerSlotKey);

    WriterUtils.writeIndexSwitchBlock(
        body,
        "headerSlotKey.getName()",
        idx,
        (offset, i) -> {
          if (i - offset == headerFieldSet.headerFields.size()) {
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

  @Override
  CodeBlock buildFeedIndexedBlock() {
    NameFactory nameFactory = context.getNameFactory();
    List<FieldDiscriminate> discriminate = schema.discriminate();

    CodeBlock.Builder body = CodeBlock.builder();
    body.beginControlFlow("switch (index)");

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

    return body.build();
  }

  @Override
  CodeBlock buildResetBlock() {
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

  @Override
  List<FieldModel> getFields() {
    return schema.discriminate()
        .stream()
        .filter(f -> !f.isHeader())
        .flatMap(f -> {
          FieldDiscriminate.SingleField singleField = (FieldDiscriminate.SingleField) f;
          return Stream.of(singleField.getField());
        })
        .collect(Collectors.toList());
  }
}
