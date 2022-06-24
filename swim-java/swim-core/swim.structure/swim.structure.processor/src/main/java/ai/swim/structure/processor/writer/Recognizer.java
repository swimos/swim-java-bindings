package ai.swim.structure.processor.writer;

import ai.swim.structure.annotations.AutoForm;
import ai.swim.structure.annotations.AutoloadedRecognizer;
import ai.swim.structure.processor.context.NameFactory;
import ai.swim.structure.processor.context.ScopedContext;
import ai.swim.structure.processor.recognizer.ClassMap;
import ai.swim.structure.processor.recognizer.RecognizerInstance;
import ai.swim.structure.processor.recognizer.StructuralRecognizer;
import ai.swim.structure.processor.schema.ClassSchema;
import ai.swim.structure.processor.schema.FieldModel;
import ai.swim.structure.processor.schema.HeaderSet;
import ai.swim.structure.processor.schema.PartitionedFields;
import com.squareup.javapoet.*;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static ai.swim.structure.processor.writer.PolymorphicRecognizer.buildPolymorphicRecognizer;
import static ai.swim.structure.processor.writer.WriterUtils.typeParametersToTypeVariable;

public class Recognizer {

  public static final String TYPE_READ_EVENT = "ai.swim.recon.event.ReadEvent";
  public static final String RECOGNIZING_BUILDER_CLASS = "ai.swim.structure.RecognizingBuilder";

  public static final String RECOGNIZER_CLASS = "ai.swim.structure.recognizer.Recognizer";
  public static final String STRUCTURAL_RECOGNIZER_CLASS = "ai.swim.structure.recognizer.structural.StructuralRecognizer";
  public static final String LABELLED_CLASS_RECOGNIZER = "ai.swim.structure.recognizer.structural.labelled.LabelledClassRecognizer";
  public static final String DELEGATE_CLASS_RECOGNIZER = "ai.swim.structure.recognizer.structural.delegate.DelegateClassRecognizer";

  public static final String FIXED_TAG_SPEC = "ai.swim.structure.recognizer.structural.tag.FixedTagSpec";
  public static final String FIELD_TAG_SPEC = "ai.swim.structure.recognizer.structural.tag.FieldTagSpec";
  public static final String LABELLED_ITEM_FIELD_KEY = "ai.swim.structure.recognizer.structural.labelled.LabelledFieldKey.ItemFieldKey";
  public static final String LABELLED_ATTR_FIELD_KEY = "ai.swim.structure.recognizer.structural.labelled.LabelledFieldKey.AttrFieldKey";
  public static final String DELEGATE_HEADER_SLOT_KEY = "ai.swim.structure.recognizer.structural.delegate.HeaderFieldKey.HeaderSlotKey";
  public static final String DELEGATE_ORDINAL_ATTR_KEY = "ai.swim.structure.recognizer.structural.delegate.OrdinalFieldKey.OrdinalFieldKeyAttr";

  public static void writeRecognizer(ClassSchema schema, ScopedContext context) throws IOException {
    ClassMap classMap = schema.getClassMap();
    List<StructuralRecognizer> subTypes = classMap.getSubTypes();
    NameFactory nameFactory = context.getNameFactory();
    TypeSpec typeSpec;

    if (classMap.isAbstract()) {
      typeSpec = buildPolymorphicRecognizer(subTypes, context).build();
    } else if (!subTypes.isEmpty()) {
      TypeSpec.Builder concreteRecognizer = writeClassRecognizer(true, schema, context);
      subTypes.add(new RecognizerInstance(nameFactory.concreteRecognizerClassName()));

      TypeSpec.Builder classRecognizer = buildPolymorphicRecognizer(subTypes, context);
      classRecognizer.addType(concreteRecognizer.build());

      typeSpec = classRecognizer.build();
    } else {
      typeSpec = writeClassRecognizer(false, schema, context).build();
    }

    JavaFile javaFile = JavaFile.builder(schema.getDeclaredPackage().getQualifiedName().toString(), typeSpec).build();
    javaFile.writeTo(context.getProcessingEnvironment().getFiler());
  }

  private static TypeSpec.Builder writeClassRecognizer(boolean isPolymorphic, ClassSchema schema, ScopedContext context) {
    NameFactory nameFactory = context.getNameFactory();
    Modifier modifier = isPolymorphic ? Modifier.PRIVATE : Modifier.PUBLIC;
    String className = isPolymorphic ? nameFactory.concreteRecognizerClassName() : nameFactory.recognizerClassName();

    TypeSpec.Builder classSpec = TypeSpec.classBuilder(className)
        .addModifiers(modifier, Modifier.FINAL)
        .addTypeVariables(typeParametersToTypeVariable(schema.getTypeParameters()));

    if (isPolymorphic) {
      classSpec.addModifiers(Modifier.STATIC);
    } else {
      AnnotationSpec recognizerAnnotationSpec = AnnotationSpec.builder(AutoloadedRecognizer.class)
          .addMember("value", "$L.class", schema.qualifiedName())
          .build();
      classSpec.addAnnotation(recognizerAnnotationSpec);
    }

    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Elements elementUtils = processingEnvironment.getElementUtils();
    Types typeUtils = processingEnvironment.getTypeUtils();

    TypeElement superclassRecognizerTypeElement = elementUtils.getTypeElement(STRUCTURAL_RECOGNIZER_CLASS);
    DeclaredType superclassRecognizerType = typeUtils.getDeclaredType(superclassRecognizerTypeElement, context.getRoot().asType());
    TypeName superclassRecognizerTypeName = TypeName.get(superclassRecognizerType);

    TypeElement recognizerTypeElement = elementUtils.getTypeElement(RECOGNIZER_CLASS);
    DeclaredType recognizerType = typeUtils.getDeclaredType(recognizerTypeElement, context.getRoot().asType());
    TypeName recognizerTypeName = TypeName.get(recognizerType);

    classSpec.superclass(superclassRecognizerTypeName);
    classSpec.addField(FieldSpec.builder(recognizerTypeName, "recognizer", Modifier.PRIVATE).build());
    classSpec.addMethods(buildConstructors(schema, context));
    classSpec.addMethods(Arrays.asList(buildMethods(context, className)));

    BuilderWriter.write(classSpec, schema, context);

    return classSpec;
  }

  private static MethodSpec[] buildMethods(ScopedContext context, String className) {
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Elements elementUtils = processingEnvironment.getElementUtils();
    Types typeUtils = processingEnvironment.getTypeUtils();

    TypeElement recognizerTypeElement = elementUtils.getTypeElement(RECOGNIZER_CLASS);
    DeclaredType typedRecognizer = typeUtils.getDeclaredType(recognizerTypeElement, context.getRoot().asType());
    TypeElement typeElement = elementUtils.getTypeElement(TYPE_READ_EVENT);

    MethodSpec[] methods = new MethodSpec[8];

    methods[0] = buildPolymorphicMethod(TypeName.get(typedRecognizer), "feedEvent", ParameterSpec.builder(TypeName.get(typeElement.asType()), "event").build(), CodeBlock.of(
        "this.recognizer = this.recognizer.feedEvent(event);" +
            "\n\tif (this.recognizer.isError()) {\nreturn Recognizer.error(this.recognizer.trap());"
            + "\n}" +
            "\nreturn this;"
    ));
    methods[1] = buildPolymorphicMethod(TypeName.get(boolean.class), "isCont", null, CodeBlock.of("return this.recognizer.isCont();"));
    methods[2] = buildPolymorphicMethod(TypeName.get(boolean.class), "isDone", null, CodeBlock.of("return this.recognizer.isDone();"));
    methods[3] = buildPolymorphicMethod(TypeName.get(boolean.class), "isError", null, CodeBlock.of("return this.recognizer.isError();"));
    methods[4] = buildPolymorphicMethod(TypeName.get(context.getRoot().asType()), "bind", null, CodeBlock.of("return this.recognizer.bind();"));
    methods[5] = buildPolymorphicMethod(TypeName.get(RuntimeException.class), "trap", null, CodeBlock.of("return this.recognizer.trap();"));
    methods[6] = buildPolymorphicMethod(TypeName.get(typedRecognizer), "reset", null, CodeBlock.of("return new $L(this.recognizer.reset());", className));
    methods[7] = buildPolymorphicMethod(TypeName.get(typedRecognizer), "asBodyRecognizer", null, CodeBlock.of("return this;"));

    return methods;
  }

  private static MethodSpec buildPolymorphicMethod(TypeName returns, String name, ParameterSpec parameterSpec, CodeBlock body) {
    MethodSpec.Builder builder = MethodSpec.methodBuilder(name)
        .returns(returns)
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .addCode(body);

    if (parameterSpec != null) {
      builder.addParameter(parameterSpec);
    }

    return builder.build();
  }

  private static List<MethodSpec> buildConstructors(ClassSchema schema, ScopedContext context) {
    List<MethodSpec> constructors = new ArrayList<>();
    constructors.add(buildDefaultConstructor(context));
    constructors.add(buildParameterisedConstructor(schema, context));
    constructors.add(buildResetConstructor(context));

    if (!schema.getTypeParameters().isEmpty()) {
      constructors.add(buildTypedConstructor(schema, context));
    }

    return constructors;
  }

  private static MethodSpec buildDefaultConstructor(ScopedContext context) {
    return MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PUBLIC)
        .addCode("this(new $L());",context.getNameFactory().builderClassName())
        .build();
  }

  private static MethodSpec buildTypedConstructor(ClassSchema schema, ScopedContext context) {
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Types typeUtils = processingEnvironment.getTypeUtils();
    Elements elementUtils = processingEnvironment.getElementUtils();

    TypeElement recognizerTypeElement = elementUtils.getTypeElement(RECOGNIZER_CLASS);

    MethodSpec.Builder methodBuilder = MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(AutoForm.TypedConstructor.class);

    List<ParameterSpec> parameters = new ArrayList<>();

    for (FieldModel fieldModel : schema.getClassMap().getFieldModels()) {
      if (fieldModel.getFieldView().isParameterised()) {
        DeclaredType typedRecognizer = typeUtils.getDeclaredType(recognizerTypeElement, fieldModel.type());
        parameters.add(ParameterSpec.builder(TypeName.get(typedRecognizer), fieldModel.fieldName()).build());
      }
    }

    CodeBlock body = CodeBlock.of("this(new $L<>($L));", context.getNameFactory().builderClassName(), parameters.stream().map(p -> p.name).collect(Collectors.joining(", ")));
    methodBuilder.addCode(body);

    return methodBuilder.addParameters(parameters).build();
  }

  private static MethodSpec buildParameterisedConstructor(ClassSchema schema, ScopedContext context) {
    MethodSpec.Builder methodBuilder = MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PRIVATE)
        .addParameter(ParameterSpec.builder(ClassName.bestGuess(context.getNameFactory().builderClassName()), "builder").build());

    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Elements elementUtils = processingEnvironment.getElementUtils();
    Types typeUtils = processingEnvironment.getTypeUtils();

    PartitionedFields partitionedFields = schema.getPartitionedFields();
    String recognizerName = partitionedFields.body.isReplaced() ? DELEGATE_CLASS_RECOGNIZER : LABELLED_CLASS_RECOGNIZER;
    CodeBlock indexFn = partitionedFields.body.isReplaced() ? buildOrdinalIndexFn(schema, context) : buildStandardIndexFn(schema, context);

    TypeElement classRecognizerElement = elementUtils.getTypeElement(recognizerName);
    DeclaredType classRecognizerDeclaredType = typeUtils.getDeclaredType(classRecognizerElement, context.getRoot().asType());

    String tag = schema.getTag();
    TypeElement fieldTagSpecElement = elementUtils.getTypeElement(tag == null ? FIELD_TAG_SPEC : FIXED_TAG_SPEC);

    CodeBlock fieldSpec = tag == null ? CodeBlock.of("new $T()", fieldTagSpecElement) : CodeBlock.of("$T(\"$L\")", fieldTagSpecElement, tag);
    String fmtArgs = String.format("this.recognizer = new $T(new %s, builder, $L, $L);", fieldSpec);

    CodeBlock body = CodeBlock.of(fmtArgs, classRecognizerDeclaredType, schema.getPartitionedFields().count(), indexFn);

    return methodBuilder.addCode(body).build();
  }

  private static CodeBlock buildOrdinalIndexFn(ClassSchema schema, ScopedContext context) {
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Elements elementUtils = processingEnvironment.getElementUtils();

    PartitionedFields partitionedFields = schema.getPartitionedFields();
    HeaderSet headerFieldSet = partitionedFields.headerSet;

    int idx = 0;
    CodeBlock.Builder body = CodeBlock.builder();
    body.beginControlFlow("(key) ->");

    if (headerFieldSet.hasTagName()) {
      idx += 1;
      body.beginControlFlow("if (key.isTag())");
      body.addStatement("return 0");
      body.endControlFlow();
    }

    if (headerFieldSet.hasTagBody() || !headerFieldSet.headerFields.isEmpty()) {
      body.beginControlFlow("if (key.isHeader())");
      body.addStatement("return $L", idx);
      body.endControlFlow();

      idx += 1;
    }

    if (!headerFieldSet.attributes.isEmpty()) {
      TypeElement attrKey = elementUtils.getTypeElement(DELEGATE_ORDINAL_ATTR_KEY);

      body.beginControlFlow("if (key.isAttr())");
      body.addStatement("$T attrKey = ($T) key", attrKey, attrKey);

      WriterUtils.writeIndexSwitchBlock(
          body,
          "attrKey.getName()",
          idx,
          (offset, i) -> {
            if (i - offset == headerFieldSet.attributes.size()) {
              return null;
            } else {
              FieldModel recognizer = headerFieldSet.attributes.get(i - offset);
              return String.format("case \"%s\":\r\n\t return %s;\r\n", recognizer.propertyName(), i);
            }
          }
      );

      body.endControlFlow();
    }

    body.beginControlFlow("if (key.isFirstItem())");
    body.addStatement("return $L", idx + headerFieldSet.attributes.size());
    body.endControlFlow();

    body.addStatement("return null");
    body.endControlFlow();

    return body.build();
  }

  private static CodeBlock buildStandardIndexFn(ClassSchema schema, ScopedContext context) {
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Elements elementUtils = processingEnvironment.getElementUtils();

    CodeBlock.Builder body = CodeBlock.builder();
    body.beginControlFlow("(key) ->");

    PartitionedFields partitionedFields = schema.getPartitionedFields();
    HeaderSet headerSet = partitionedFields.headerSet;

    int idx = 0;

    if (headerSet.hasTagBody() || partitionedFields.hasHeaderFields()) {
      body.beginControlFlow("if (key.isHeader())");
      body.addStatement("return $L", idx);
      body.endControlFlow();
      idx += 1;
    }

    if (!headerSet.attributes.isEmpty()) {
      body.beginControlFlow("if (key.isAttribute())");
      TypeElement attrFieldKeyElement = elementUtils.getTypeElement(LABELLED_ATTR_FIELD_KEY);

      body.addStatement("$T attrFieldKey = ($T) key", attrFieldKeyElement, attrFieldKeyElement);
      body.beginControlFlow("switch (attrFieldKey.getKey())");

      int attrCount = headerSet.attributes.size();

      for (int i = 0; i < attrCount; i++) {
        FieldModel recognizer = headerSet.attributes.get(i);

        body.add("case \"$L\":", recognizer.propertyName());
        body.addStatement("\t return $L", i + idx);
      }

      body.endControlFlow();
      body.endControlFlow();

      idx += attrCount;
    }

    List<FieldModel> items = partitionedFields.body.getFields();

    if (!items.isEmpty()) {
      body.beginControlFlow("if (key.isItem())");

      TypeElement itemFieldKeyElement = elementUtils.getTypeElement(LABELLED_ITEM_FIELD_KEY);
      body.addStatement("$T itemFieldKey = ($T) key", itemFieldKeyElement, itemFieldKeyElement);
      body.beginControlFlow("switch (itemFieldKey.getName())");

      for (int i = 0; i < items.size(); i++) {
        FieldModel recognizer = items.get(i);

        body.add("case \"$L\":", recognizer.propertyName());
        body.addStatement("\t return $L", i + idx);
      }

      body.add("default:");
      body.addStatement("\tthrow new RuntimeException(\"Unexpected key: \" + key)");
      body.endControlFlow();
      body.endControlFlow();
    }

    body.addStatement("return null");
    body.endControlFlow();

    return body.build();
  }

  private static MethodSpec buildResetConstructor(ScopedContext context) {
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Elements elementUtils = processingEnvironment.getElementUtils();
    Types typeUtils = processingEnvironment.getTypeUtils();

    MethodSpec.Builder methodBuilder = MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE);

    TypeElement recognizerTypeElement = elementUtils.getTypeElement(RECOGNIZER_CLASS);
    DeclaredType typedRecognizer = typeUtils.getDeclaredType(recognizerTypeElement, context.getRoot().asType());

    methodBuilder.addParameter(TypeName.get(typedRecognizer), "recognizer");
    methodBuilder.addStatement("this.recognizer = recognizer");

    return methodBuilder.build();
  }

}
