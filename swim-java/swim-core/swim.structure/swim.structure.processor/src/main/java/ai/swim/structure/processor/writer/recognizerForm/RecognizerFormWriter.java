package ai.swim.structure.processor.writer.recognizerForm;

import ai.swim.structure.processor.model.ClassLikeModel;
import ai.swim.structure.processor.model.InterfaceModel;
import ai.swim.structure.processor.model.ModelInspector;
import ai.swim.structure.processor.writer.Writer;
import ai.swim.structure.processor.writer.recognizerForm.recognizer.PolymorphicRecognizer;
import ai.swim.structure.processor.writer.recognizerForm.recognizer.Recognizer;
import com.squareup.javapoet.*;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static ai.swim.structure.processor.writer.recognizerForm.Lookups.RECOGNIZER_PROXY;

public class RecognizerFormWriter implements Writer {
  private final ProcessingEnvironment environment;
  private final ModelInspector inspector;

  public RecognizerFormWriter(ProcessingEnvironment environment, ModelInspector inspector) {
    this.environment = environment;
    this.inspector = inspector;
  }

  public static List<ParameterSpec> writeGenericRecognizerConstructor(List<? extends TypeParameterElement> typeParameters, RecognizerContext context) {
    Types typeUtils = context.getTypeUtils();
    Elements elementUtils = context.getElementUtils();

    List<ParameterSpec> parameters = new ArrayList<>(typeParameters.size());
    TypeElement typeParameterElement = elementUtils.getTypeElement(Lookups.TYPE_PARAMETER);

    for (TypeParameterElement typeParameter : typeParameters) {
      DeclaredType typed = typeUtils.getDeclaredType(typeParameterElement, typeParameter.asType());
      parameters.add(ParameterSpec.builder(TypeName.get(typed), context.getFormatter().typeParameterName(typeParameter.toString())).build());
    }

    return parameters;
  }

  @Override
  public void writeClass(ClassLikeModel model) throws IOException {
    RecognizerContext context = RecognizerContext.build(model.getElement(), environment, inspector, model.getJavaClassName(), model.getDeclaredPackage());
    Recognizer.writeRecognizer(model, context);
  }

  @Override
  public void writeInterface(InterfaceModel model) throws IOException {
    RecognizerContext context = RecognizerContext.build(model.getElement(), environment, inspector, model.getJavaClassName(), model.getDeclaredPackage());
    TypeSpec typeSpec = PolymorphicRecognizer.buildPolymorphicRecognizer(context, model.getSubTypes())
            .build();
    JavaFile javaFile = JavaFile.builder(model.getDeclaredPackage().getQualifiedName().toString(), typeSpec)
            .addStaticImport(ClassName.bestGuess(RECOGNIZER_PROXY), "getProxy")
            .build();

    javaFile.writeTo(context.getProcessingEnvironment().getFiler());
  }

}
