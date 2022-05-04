package ai.swim.structure.processor.writer;

import ai.swim.structure.processor.context.ScopedContext;
import ai.swim.structure.processor.structure.ClassSchema;
import ai.swim.structure.processor.structure.recognizer.ElementRecognizer;
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

import static ai.swim.structure.processor.writer.RecognizerWriter.RECOGNIZING_BUILDER_CLASS;
import static ai.swim.structure.processor.writer.RecognizerWriter.TYPE_READ_EVENT;

public class BuilderWriter {
  private static final String FIELD_RECOGNIZING_BUILDER_CLASS = "ai.swim.structure.FieldRecognizingBuilder";
  private static final String RECOGNIZING_BUILDER_FEED_INDEX = "feedIndexed";
  private static final String RECOGNIZING_BUILDER_BIND = "bind";

  public static void write(ClassSchema schema, ScopedContext context) throws IOException {
    ProcessingEnvironment processingEnvironment = context.getProcessingContext().getProcessingEnvironment();
    Types typeUtils = processingEnvironment.getTypeUtils();
    Elements elementUtils = processingEnvironment.getElementUtils();

    TypeSpec.Builder classSpec = TypeSpec.classBuilder(schema.className() + "Builder")
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

    TypeElement recognizingBuilderElement = elementUtils.getTypeElement(RECOGNIZING_BUILDER_CLASS);
    DeclaredType recognizingBuilderType = typeUtils.getDeclaredType(recognizingBuilderElement, context.getRoot().asType());

    classSpec.addSuperinterface(TypeName.get(recognizingBuilderType));
    classSpec.addMethod(buildConstructor());
    classSpec.addFields(Arrays.asList(buildFields(schema, processingEnvironment)));
    classSpec.addMethods(Arrays.asList(buildMethods(schema, context)));

    JavaFile javaFile = JavaFile.builder(schema.getPackageElement().getQualifiedName().toString(), classSpec.build()).build();
    javaFile.writeTo(context.getProcessingContext().getProcessingEnvironment().getFiler());
  }

  private static MethodSpec[] buildMethods(ClassSchema schema, ScopedContext context) {
    MethodSpec[] methods = new MethodSpec[2];
    methods[0] = buildFeedIndexed(schema, context.getProcessingContext().getProcessingEnvironment());
    methods[1] = buildBind(schema, context);

    return methods;
  }

  private static String buildFieldName(String fieldName) {
    return fieldName + "Builder";
  }

  private static MethodSpec buildFeedIndexed(ClassSchema schema, ProcessingEnvironment processingEnvironment) {
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

    List<ElementRecognizer> recognizers = schema.getRecognizers();

    for (int i = 0; i < recognizers.size(); i++) {
      ElementRecognizer recognizer = recognizers.get(i);
      body.add("case $L:", i);
      body.addStatement("\nreturn this.$L.feed(event)", buildFieldName(recognizer.fieldName()));
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

    for (ElementRecognizer recognizer : schema.getRecognizers()) {
      recognizer.getAccessor().write(body, "obj", String.format("this.%s.bind()", buildFieldName(recognizer.fieldName())));
    }

    body.add("\nreturn obj;");
    builder.addCode(body.build());

    return builder.build();
  }

  private static MethodSpec buildConstructor() {
    return MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC).build();
  }

  private static FieldSpec[] buildFields(ClassSchema schema, ProcessingEnvironment processingEnvironment) {
    Elements elementUtils = processingEnvironment.getElementUtils();
    Types typeUtils = processingEnvironment.getTypeUtils();
    TypeElement fieldFieldRecognizingBuilder = elementUtils.getTypeElement(FIELD_RECOGNIZING_BUILDER_CLASS);

    List<ElementRecognizer> recognizers = schema.getRecognizers();
    int recognizerCount = recognizers.size();
    FieldSpec[] fields = new FieldSpec[recognizerCount];

    for (int i = 0; i < recognizerCount; i++) {
      ElementRecognizer recognizer = recognizers.get(i);

      TypeMirror recognizerType = recognizer.type();

      if (recognizer.type().getKind().isPrimitive()) {
        TypeElement boxedClass = typeUtils.boxedClass((PrimitiveType) recognizer.type());
        recognizerType = boxedClass.asType();
      }

      DeclaredType memberRecognizingBuilder = typeUtils.getDeclaredType(fieldFieldRecognizingBuilder, recognizerType);
      FieldSpec.Builder fieldSpec = FieldSpec.builder(TypeName.get(memberRecognizingBuilder), buildFieldName(recognizer.fieldName()), Modifier.PRIVATE, Modifier.FINAL);

      fieldSpec.initializer(CodeBlock.of("new $L<>($L)", FIELD_RECOGNIZING_BUILDER_CLASS, recognizer.initializer()));

      fields[i] = fieldSpec.build();
    }

    return fields;
  }

}
