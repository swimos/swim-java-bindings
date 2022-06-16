package ai.swim.structure.processor.recognizer;

import ai.swim.structure.processor.context.ScopedContext;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeKind;
import java.util.Collection;
import java.util.Map;

import static ai.swim.structure.processor.ElementUtils.isSubType;

public abstract class RecognizerModel {

  public static RecognizerModel from(Element element, ScopedContext context) {
    RecognizerModel recognizer = RecognizerModel.fromPrimitiveType(element);

    if (recognizer != null) {
      return recognizer;
    }

    recognizer = RecognizerModel.fromStdType(element, context);

    if (recognizer != null) {
      return recognizer;
    }

    recognizer = context.getRecognizerFactory().lookup(element);

    if (recognizer != null) {
      return recognizer;
    }

    // We're out of options now. The recognizer isn't available to us now, so we'll have to hope that it's been
    // registered with the recognizer proxy for a runtime lookup, or it will be derived at runtime and incur the penalty
    // of reflection.
    return RecognizerReference.lookupAny(element.asType());
  }

  private static RecognizerModel fromStdType(Element element, ScopedContext context) {
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();

    if (isSubType(processingEnvironment, element, Collection.class)) {
      return ListRecognizerModel.from(element, context);
    }

    if (isSubType(processingEnvironment, element, Map.class)) {
//      return ClassRecognizerModel.map(element, context);
      throw new AssertionError("Map implementation");
    }

    return null;
  }

  public static RecognizerModel fromPrimitiveType(Element element) {
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

  public abstract String recognizerInitializer();

  public Object defaultValue() {
    return null;
  }

}
