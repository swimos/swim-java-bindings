package ai.swim.structure.processor.writer.recognizer;

import ai.swim.structure.annotations.AutoForm;
import ai.swim.structure.annotations.AutoloadedRecognizer;
import ai.swim.structure.processor.context.NameFactory;
import ai.swim.structure.processor.context.ScopedContext;
import ai.swim.structure.processor.recognizer.ClassMap;
import ai.swim.structure.processor.recognizer.RecognizerModel;
import ai.swim.structure.processor.schema.ClassSchema;
import ai.swim.structure.processor.schema.FieldModel;
import ai.swim.structure.processor.schema.HeaderSet;
import ai.swim.structure.processor.schema.PartitionedFields;
import ai.swim.structure.processor.writer.WriterUtils;
import ai.swim.structure.processor.writer.builder.BuilderWriter;
import com.squareup.javapoet.*;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static ai.swim.structure.processor.writer.Lookups.*;
import static ai.swim.structure.processor.writer.WriterUtils.typeParametersToTypeVariable;
import static ai.swim.structure.processor.writer.WriterUtils.writeGenericRecognizerConstructor;
import static ai.swim.structure.processor.writer.recognizer.PolymorphicRecognizer.buildPolymorphicRecognizer;

public class Recognizer {


  public static void writeRecognizer(ClassSchema schema, ScopedContext context) throws IOException {
    ClassMap classMap = schema.getClassMap();
    List<RecognizerModel> subTypes = classMap.getSubTypes();
    NameFactory nameFactory = context.getNameFactory();

    TypeSpec typeSpec;

    if (classMap.isAbstract()) {
      typeSpec = buildPolymorphicRecognizer(subTypes, context).build();
    } else if (!subTypes.isEmpty()) {
      TypeSpec.Builder concreteRecognizer = writeClassRecognizer(true, schema, context);
      subTypes.add(new RecognizerModel(null, null) {
        @Override
        public CodeBlock initializer(ScopedContext context,boolean inConstructor) {
          return CodeBlock.of("new $L()", nameFactory.concreteRecognizerClassName());
        }
      });

      TypeSpec.Builder classRecognizer = buildPolymorphicRecognizer(subTypes, context);
      classRecognizer.addType(concreteRecognizer.build());

      typeSpec = classRecognizer.build();
    } else {
      typeSpec = writeClassRecognizer(false, schema, context).build();
    }

    JavaFile javaFile = JavaFile.builder(schema.getDeclaredPackage().getQualifiedName().toString(), typeSpec)
        .addStaticImport(Objects.class, "requireNonNullElse")
        .addStaticImport(ClassName.bestGuess(RECOGNIZER_PROXY), "getProxy")
        .build();
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
    classSpec.addMethods(buildMethods(context, className));

    BuilderWriter.write(classSpec, schema, context);

    return classSpec;
  }

  private static List<MethodSpec> buildMethods(ScopedContext context, String className) {
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Elements elementUtils = processingEnvironment.getElementUtils();
    Types typeUtils = processingEnvironment.getTypeUtils();

    TypeElement recognizerTypeElement = elementUtils.getTypeElement(RECOGNIZER_CLASS);
    DeclaredType typedRecognizer = typeUtils.getDeclaredType(recognizerTypeElement, context.getRoot().asType());
    TypeElement typeElement = elementUtils.getTypeElement(TYPE_READ_EVENT);

    List<MethodSpec> methods = new ArrayList<>();

    methods.add(buildPolymorphicMethod(TypeName.get(typedRecognizer), "feedEvent", ParameterSpec.builder(TypeName.get(typeElement.asType()), "event").build(), CodeBlock.of(
        "this.recognizer = this.recognizer.feedEvent(event);" +
            "\n\tif (this.recognizer.isError()) {\nreturn Recognizer.error(this.recognizer.trap());"
            + "\n}" +
            "\nreturn this;"
    )));
    methods.add(buildPolymorphicMethod(TypeName.get(boolean.class), "isCont", null, CodeBlock.of("return this.recognizer.isCont();")));
    methods.add(buildPolymorphicMethod(TypeName.get(boolean.class), "isDone", null, CodeBlock.of("return this.recognizer.isDone();")));
    methods.add(buildPolymorphicMethod(TypeName.get(boolean.class), "isError", null, CodeBlock.of("return this.recognizer.isError();")));
    methods.add(buildPolymorphicMethod(TypeName.get(context.getRoot().asType()), "bind", null, CodeBlock.of("return this.recognizer.bind();")));
    methods.add(buildPolymorphicMethod(TypeName.get(RuntimeException.class), "trap", null, CodeBlock.of("return this.recognizer.trap();")));
    methods.add(buildPolymorphicMethod(TypeName.get(typedRecognizer), "reset", null, CodeBlock.of("return new $L();", className)));
    methods.add(buildPolymorphicMethod(TypeName.get(typedRecognizer), "asBodyRecognizer", null, CodeBlock.of("return this;")));

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

    if (!schema.getTypeParameters().isEmpty()) {
      constructors.add(buildTypedConstructor(schema, context));
    }

    return constructors;
  }

  private static MethodSpec buildDefaultConstructor(ScopedContext context) {
    return MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PUBLIC)
        .addCode("this(new $L());", context.getNameFactory().builderClassName())
        .build();
  }

  private static MethodSpec buildTypedConstructor(ClassSchema schema, ScopedContext context) {
    MethodSpec.Builder methodBuilder = MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(AutoForm.TypedConstructor.class);

    List<ParameterSpec> parameters = writeGenericRecognizerConstructor(schema.getTypeParameters(), context);

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

}
