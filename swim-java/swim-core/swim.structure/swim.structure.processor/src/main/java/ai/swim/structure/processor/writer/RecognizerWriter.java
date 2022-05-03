package ai.swim.structure.processor.writer;

import ai.swim.structure.processor.context.ScopedContext;
import ai.swim.structure.processor.structure.ClassSchema;
import ai.swim.structure.processor.structure.recognizer.ElementRecognizer;
import com.squareup.javapoet.*;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.io.IOException;

public class RecognizerWriter {

  public static final String TYPE_READ_EVENT = "ai.swim.recon.event.ReadEvent";

  public static void writeRecognizer(ClassSchema schema, ScopedContext context) throws IOException {
    writeBuilder(schema, context);
  }

  private static void writeBuilder(ClassSchema schema, ScopedContext context) throws IOException {
    PackageElement packageElement = schema.getPackageElement();
    ProcessingEnvironment processingEnvironment = context.getProcessingContext().getProcessingEnvironment();

    Elements elementUtils = processingEnvironment.getElementUtils();
    Types typeUtils = processingEnvironment.getTypeUtils();

//    TypeElement typeElement = elementUtils.getTypeElement("ai.swim.structure.RecognizingBuilder");
//    DeclaredType declaredType = typeUtils.getDeclaredType(typeElement, context.getRoot().getEnclosingType());

    TypeSpec.Builder classSpec = TypeSpec.classBuilder(schema.className() + "Builder").addModifiers(Modifier.PUBLIC, Modifier.FINAL);
    TypeElement fieldFieldRecognizingBuilder = elementUtils.getTypeElement("ai.swim.structure.FieldRecognizingBuilder");

    for (ElementRecognizer recognizer : schema.getRecognizers()) {
      System.out.println("Looking up recognizer: " + recognizer);

      TypeMirror recognizerType = recognizer.type();

      if (recognizer.type().getKind().isPrimitive()) {
        TypeElement boxedClass = typeUtils.boxedClass((PrimitiveType) recognizer.type());
        recognizerType = boxedClass.asType();
      }

      DeclaredType memberRecognizingBuilder = typeUtils.getDeclaredType(fieldFieldRecognizingBuilder, recognizerType);
      FieldSpec.Builder fieldSpec = FieldSpec.builder(TypeName.get(memberRecognizingBuilder), recognizer.fieldName() + "Builder", Modifier.PRIVATE, Modifier.FINAL);

      System.out.println("Writing code block with init: " + recognizer.initializer());

      fieldSpec.initializer(CodeBlock.of("new ai.swim.structure.FieldRecognizingBuilder<>($L)", recognizer.initializer()));
      classSpec.addField(fieldSpec.build());
    }

    JavaFile javaFile = JavaFile.builder(packageElement.getQualifiedName().toString(), classSpec.build()).build();
//    javaFile.writeTo(processingEnvironment.getFiler());
    javaFile.writeTo(System.out);
  }




}
