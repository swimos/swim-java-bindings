package ai.swim.structure.processor.writer;

import ai.swim.structure.annotations.AutoloadedRecognizer;
import ai.swim.structure.processor.context.ScopedContext;
import ai.swim.structure.processor.recognizer.RecognizerModel;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.io.IOException;
import java.util.List;

public class PolymorphicClassRecognizer {

  public static final String POLYMORPHIC_CLASS_RECOGNIZER = "ai.swim.structure.recognizer.structural.PolymorphicClassRecognizer";

  public static TypeSpec.Builder buildPolymorphicClassRecognizer(List<RecognizerModel> subTypes, ScopedContext context) throws IOException {
    AnnotationSpec recognizerAnnotationSpec = AnnotationSpec.builder(AutoloadedRecognizer.class)
        .addMember("value", "$T.class", context.getRoot().asType())
        .build();

    TypeSpec.Builder classSpec = TypeSpec.classBuilder(context.getNameFactory().recognizerClassName())
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        .addAnnotation(recognizerAnnotationSpec);

    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Elements elementUtils = processingEnvironment.getElementUtils();
    Types typeUtils = processingEnvironment.getTypeUtils();

    TypeElement recognizerTypeElement = elementUtils.getTypeElement(POLYMORPHIC_CLASS_RECOGNIZER);
    DeclaredType recognizerType = typeUtils.getDeclaredType(recognizerTypeElement, context.getRoot().asType());
    classSpec.superclass(TypeName.get(recognizerType));

    MethodSpec constructor = MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PUBLIC)
        .addStatement("super($L)", buildInitializer(subTypes))
        .build();
    classSpec.addMethod(constructor);

    return classSpec;
  }

  private static String buildInitializer(List<RecognizerModel> subTypes) {
    StringBuilder initializer = new StringBuilder("java.util.List.of(");

    for (int i = 0; i < subTypes.size(); i++) {
      boolean fin = i + 1 >= subTypes.size();
      RecognizerModel recognizerModel = subTypes.get(i);

      initializer.append(recognizerModel.recognizerInitializer()).append(fin ? "" : ", ");
    }

    return initializer.append(")").toString();
  }

}
