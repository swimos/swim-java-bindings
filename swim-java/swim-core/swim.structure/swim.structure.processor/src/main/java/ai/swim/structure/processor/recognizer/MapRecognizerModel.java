package ai.swim.structure.processor.recognizer;

import ai.swim.structure.processor.Utils;
import ai.swim.structure.processor.context.ScopedContext;
import com.squareup.javapoet.CodeBlock;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.List;
import java.util.Map;

import static ai.swim.structure.processor.Utils.unrollType;
import static ai.swim.structure.processor.writer.Lookups.MAP_RECOGNIZER_CLASS;

public class MapRecognizerModel extends StructuralRecognizer {
  private final RecognizerModel keyRecognizer;
  private final RecognizerModel valueRecognizer;

  private MapRecognizerModel(TypeMirror type, RecognizerModel keyRecognizer, RecognizerModel valueRecognizer) {
    super(type);
    this.keyRecognizer = keyRecognizer;
    this.valueRecognizer = valueRecognizer;
  }

  public static StructuralRecognizer from(TypeMirror typeMirror, ScopedContext context) {
    DeclaredType variableType = (DeclaredType) typeMirror;
    List<? extends TypeMirror> typeArguments = variableType.getTypeArguments();

    if (typeArguments.size() != 2) {
      throw new IllegalArgumentException("Attempted to build a map from " + typeArguments.size() + " type parameters");
    }

    TypeMirror keyType = typeArguments.get(0);
    TypeMirror valueType = typeArguments.get(1);

    Utils.UnrolledType unrolledKey = unrollType(context, keyType);
    Utils.UnrolledType unrolledValue = unrollType(context, valueType);

    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Elements elementUtils = processingEnvironment.getElementUtils();

    Types typeUtils = processingEnvironment.getTypeUtils();
    TypeElement typeElement = elementUtils.getTypeElement(Map.class.getCanonicalName());
    DeclaredType newMapType = typeUtils.getDeclaredType(typeElement, unrolledKey.typeMirror, unrolledValue.typeMirror);

    return new MapRecognizerModel(newMapType, unrolledKey.recognizerModel, unrolledValue.recognizerModel);
  }

  @Override
  public CodeBlock initializer(ScopedContext context, boolean inConstructor) {
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();
    Elements elementUtils = processingEnvironment.getElementUtils();
    Types typeUtils = processingEnvironment.getTypeUtils();

    TypeElement typeElement = elementUtils.getTypeElement(MAP_RECOGNIZER_CLASS);
    TypeMirror erased = typeUtils.erasure(typeElement.asType());

    return CodeBlock.of("new $T<>($L, $L)", erased, keyRecognizer.initializer(context, inConstructor), valueRecognizer.initializer(context, inConstructor));
  }

  @Override
  public TypeMirror type(ProcessingEnvironment environment) {
    return this.type;
  }

}
