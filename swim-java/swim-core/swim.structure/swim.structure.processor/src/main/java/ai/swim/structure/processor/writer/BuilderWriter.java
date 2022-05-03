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

import static ai.swim.structure.processor.writer.RecognizerWriter.TYPE_READ_EVENT;

public class BuilderWriter {
  private static final String FIELD_RECOGNIZING_BUILDER_CLASS = "ai.swim.structure.FieldRecognizingBuilder";
  private static final String RECOGNIZING_BUILDER_FEED_INDEX = "feedIndexed";
  private static final String RECOGNIZING_BUILDER_BIND = "bind";

  public static void write(ClassSchema schema, ScopedContext context) throws IOException {
    ProcessingEnvironment processingEnvironment = context.getProcessingContext().getProcessingEnvironment();
    TypeSpec.Builder classSpec = TypeSpec.classBuilder(schema.className() + "Builder").addModifiers(Modifier.PUBLIC, Modifier.FINAL);

    classSpec.addMethod(buildConstructor());
    classSpec.addFields(Arrays.asList(buildFields(schema, processingEnvironment)));
    classSpec.addMethods(Arrays.asList(buildMethods(schema, processingEnvironment)));

    JavaFile javaFile = JavaFile.builder(schema.getPackageElement().getQualifiedName().toString(), classSpec.build()).build();
    javaFile.writeTo(System.out);
  }

  private static MethodSpec[] buildMethods(ClassSchema schema, ProcessingEnvironment processingEnvironment) {
    MethodSpec[] methods = new MethodSpec[2];
    methods[0] = buildFeedIndexed(schema, processingEnvironment);
    methods[1] = buildBind(schema, processingEnvironment);

    return methods;
  }

  private static MethodSpec buildBind(ClassSchema schema, ProcessingEnvironment processingEnvironment) {
    Elements elementUtils = processingEnvironment.getElementUtils();
    Types typeUtils = processingEnvironment.getTypeUtils();

    TypeElement readEventType = elementUtils.getTypeElement(TYPE_READ_EVENT);

    MethodSpec.Builder builder = MethodSpec.methodBuilder(RECOGNIZING_BUILDER_FEED_INDEX)
        .addParameter(Integer.TYPE, "index")
        .addParameter(TypeName.get(readEventType.asType(), "event");


    return null;
  }

  private static MethodSpec buildFeedIndexed(ClassSchema schema, ProcessingEnvironment processingEnvironment) {
    return null;
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
      FieldSpec.Builder fieldSpec = FieldSpec.builder(TypeName.get(memberRecognizingBuilder), recognizer.fieldName() + "Builder", Modifier.PRIVATE, Modifier.FINAL);

      System.out.println("Writing code block with init: " + recognizer.initializer());

      fieldSpec.initializer(CodeBlock.of("new $L<>($L)", FIELD_RECOGNIZING_BUILDER_CLASS, recognizer.initializer()));

      fields[i] = fieldSpec.build();
    }

    return fields;
  }

}
