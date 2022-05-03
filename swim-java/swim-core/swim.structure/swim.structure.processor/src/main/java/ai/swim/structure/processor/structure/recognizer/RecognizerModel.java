package ai.swim.structure.processor.structure.recognizer;

import ai.swim.structure.processor.context.ScopedContext;

import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;

public abstract class RecognizerModel {

  public static RecognizerModel from(VariableElement element, ScopedContext context) {
    RecognizerModel recognizer = RecognizerModel.fromPrimitiveType(element);

    if (recognizer != null) {
      return recognizer;
    }

    return null;
  }

  public static RecognizerModel fromPrimitiveType(VariableElement element) {
    TypeKind kind = element.asType().getKind();

    switch (kind) {
      case BOOLEAN:
        return PrimitiveRecognizerModel.booleanRecognizer();
      case BYTE:
        return PrimitiveRecognizerModel.byteRecognizer();
      case SHORT:
        return PrimitiveRecognizerModel.shortRecognizer();
      case INT:
        return PrimitiveRecognizerModel.intRecognizer();
      case LONG:
        return PrimitiveRecognizerModel.longRecognizer();
      case CHAR:
        return PrimitiveRecognizerModel.charRecognizer();
      case FLOAT:
        return PrimitiveRecognizerModel.floatRecognizer();
      case DOUBLE:
        return PrimitiveRecognizerModel.doubleRecognizer();
      default:
        return null;
    }

  }

  public abstract String initializer();
}
