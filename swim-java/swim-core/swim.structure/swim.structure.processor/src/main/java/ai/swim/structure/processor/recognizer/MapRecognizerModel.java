package ai.swim.structure.processor.recognizer;

import ai.swim.structure.processor.Utils;
import ai.swim.structure.processor.context.ScopedContext;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.List;

import static ai.swim.structure.processor.Utils.unrollType;

public class MapRecognizerModel extends StructuralRecognizer {
  private final RecognizerModel keyRecognizer;
  private final RecognizerModel valueRecognizer;
  private final TypeMirror type;

  private MapRecognizerModel(TypeMirror type, RecognizerModel keyRecognizer, RecognizerModel valueRecognizer) {
    this.type = type;
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

    return new MapRecognizerModel(variableType, unrolledKey.recognizerModel, unrolledValue.recognizerModel);
  }

  @Override
  public String recognizerInitializer() {
    return String.format("new ai.swim.structure.recognizer.std.HashMapRecognizer<>(%s, %s)",
        this.keyRecognizer.recognizerInitializer(),
        this.valueRecognizer.recognizerInitializer()
    );
  }

  @Override
  public TypeMirror type(ProcessingEnvironment environment) {
    return this.type;
  }

  @Override
  public RecognizerModel retyped(ScopedContext context) {
    return new MapRecognizerModel(type, keyRecognizer.retyped(context), valueRecognizer.retyped(context));
  }
}
