package ai.swim.structure.processor.writer;

import ai.swim.structure.annotations.AutoloadedRecognizer;
import ai.swim.structure.processor.context.ScopedContext;
import ai.swim.structure.processor.schema.ClassSchema;
import ai.swim.structure.processor.schema.FieldModel;
import ai.swim.structure.processor.schema.PartitionedFields;
import com.squareup.javapoet.*;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class RecognizerWriter {

  static final String TYPE_READ_EVENT = "ai.swim.recon.event.ReadEvent";
  static final String RECOGNIZING_BUILDER_CLASS = "ai.swim.structure.RecognizingBuilder";

  private static final String RECOGNIZER_CLASS = "ai.swim.structure.recognizer.Recognizer";
  private static final String CLASS_RECOGNIZER_INIT = "ai.swim.structure.recognizer.structural.LabelledClassRecognizer";
  private static final String FIXED_TAG_SPEC = "ai.swim.structure.recognizer.structural.tag.FixedTagSpec";
  private static final String ITEM_FIELD_KEY = "ai.swim.structure.recognizer.structural.key.ItemFieldKey";

  public static void writeRecognizer(ClassSchema schema, ScopedContext context) throws IOException {
    BuilderWriter.write(schema, context);

    AnnotationSpec recognizerAnnotationSpec = AnnotationSpec.builder(AutoloadedRecognizer.class)
        .addMember("value", "$T.class", context.getRoot().asType())
        .build();

    TypeSpec.Builder classSpec = TypeSpec.classBuilder(schema.getRecognizerName())
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        .addAnnotation(recognizerAnnotationSpec);

    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Elements elementUtils = processingEnvironment.getElementUtils();
    Types typeUtils = processingEnvironment.getTypeUtils();

    TypeElement recognizerTypeElement = elementUtils.getTypeElement(RECOGNIZER_CLASS);
    DeclaredType recognizerType = typeUtils.getDeclaredType(recognizerTypeElement, context.getRoot().asType());
    TypeName recognizerTypeName = TypeName.get(recognizerType);

    classSpec.superclass(TypeName.get(recognizerType));
    classSpec.addField(buildRecognizerField(recognizerTypeName));
    classSpec.addMethods(Arrays.asList(buildConstructors(schema, context)));
    classSpec.addMethods(Arrays.asList(buildMethods(schema, context)));

    JavaFile javaFile = JavaFile.builder(schema.getDeclaredPackage().getQualifiedName().toString(), classSpec.build()).build();
    javaFile.writeTo(context.getProcessingEnvironment().getFiler());
  }

  private static FieldSpec buildRecognizerField(TypeName recognizerTypeName) {
    return FieldSpec.builder(recognizerTypeName, "recognizer", Modifier.PRIVATE).build();
  }

  private static MethodSpec[] buildMethods(ClassSchema schema, ScopedContext context) {
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Elements elementUtils = processingEnvironment.getElementUtils();
    Types typeUtils = processingEnvironment.getTypeUtils();

    TypeElement recognizerTypeElement = elementUtils.getTypeElement(RECOGNIZER_CLASS);
    DeclaredType typedRecognizer = typeUtils.getDeclaredType(recognizerTypeElement, context.getRoot().asType());
    TypeElement typeElement = elementUtils.getTypeElement(TYPE_READ_EVENT);

    MethodSpec[] methods = new MethodSpec[7];

    methods[0] = buildPolymorphicMethod(TypeName.get(typedRecognizer), "feedEvent", ParameterSpec.builder(TypeName.get(typeElement.asType()), "event").build(), CodeBlock.of(
        "this.recognizer = this.recognizer.feedEvent(event);\nreturn this;"
    ));
    methods[1] = buildPolymorphicMethod(TypeName.get(boolean.class), "isCont", null, CodeBlock.of("return this.recognizer.isCont();"));
    methods[2] = buildPolymorphicMethod(TypeName.get(boolean.class), "isDone", null, CodeBlock.of("return this.recognizer.isDone();"));
    methods[3] = buildPolymorphicMethod(TypeName.get(boolean.class), "isError", null, CodeBlock.of("return this.recognizer.isError();"));
    methods[4] = buildPolymorphicMethod(TypeName.get(context.getRoot().asType()), "bind", null, CodeBlock.of("return this.recognizer.bind();"));
    methods[5] = buildPolymorphicMethod(TypeName.get(RuntimeException.class), "trap", null, CodeBlock.of("return this.recognizer.trap();"));
    methods[6] = buildPolymorphicMethod(TypeName.get(typedRecognizer), "reset", null, CodeBlock.of("return new $L(this.recognizer.reset());", schema.getRecognizerName()));

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

  private static MethodSpec[] buildConstructors(ClassSchema schema, ScopedContext context) {
    MethodSpec[] constructors = new MethodSpec[2];
    constructors[0] = buildDefaultConstructor(schema, context);
    constructors[1] = buildResetConstructor(context);

    return constructors;
  }

  private static MethodSpec buildDefaultConstructor(ClassSchema schema, ScopedContext context) {
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Elements elementUtils = processingEnvironment.getElementUtils();
    Types typeUtils = processingEnvironment.getTypeUtils();

    MethodSpec.Builder methodBuilder = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);
    CodeBlock.Builder body = CodeBlock.builder();

    TypeElement classRecognizerInitElement = elementUtils.getTypeElement(CLASS_RECOGNIZER_INIT);
    DeclaredType classRecognizerDeclaredType = typeUtils.getDeclaredType(classRecognizerInitElement, context.getRoot().asType());
    TypeElement fixedTagSpecElement = elementUtils.getTypeElement(FIXED_TAG_SPEC);
    String objectBuilder = String.format("%s.%sBuilder", schema.getDeclaredPackage().getQualifiedName(), schema.getJavaClassName());

    String tag = schema.getTag();

    PartitionedFields partitionedFields = schema.getPartitionedFields();
    int fieldCount = partitionedFields.count();

    body.add("this.recognizer = new $T(new $T(\"$L\"), new $L(), $L, ",
        classRecognizerDeclaredType,
        fixedTagSpecElement,
        tag,
        objectBuilder,
        fieldCount
    );

    body.add("(key) -> {\n");
    body.beginControlFlow("if (key.isItem())");

    TypeElement itemFieldKeyElement = elementUtils.getTypeElement(ITEM_FIELD_KEY);
    body.addStatement("$T itemFieldKey = ($T) key", itemFieldKeyElement, itemFieldKeyElement);
    body.beginControlFlow("switch (itemFieldKey.getName())");


    List<FieldModel> recognizers = schema.getPartitionedFields().flatten();

    for (int i = 0; i < recognizers.size(); i++) {
      FieldModel recognizer = recognizers.get(i);

      body.add("case \"$L\":", recognizer.propertyName());
      body.addStatement("\t return $L", i);
    }

    body.add("default:");
    body.addStatement("\tthrow new RuntimeException(\"Unexpected key: \" + key)");
    body.endControlFlow();
    body.endControlFlow();
    body.addStatement("return null");
    body.add("});");

    methodBuilder.addCode(body.build());

    return methodBuilder.build();
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
