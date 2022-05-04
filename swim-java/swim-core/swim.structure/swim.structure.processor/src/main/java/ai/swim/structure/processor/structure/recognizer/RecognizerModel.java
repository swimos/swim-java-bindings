package ai.swim.structure.processor.structure.recognizer;

import ai.swim.structure.processor.context.ScopedContext;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

import static ai.swim.structure.processor.ElementUtils.isSubType;

public abstract class RecognizerModel {

  public static RecognizerModel from(VariableElement element, ScopedContext context) {
    RecognizerModel recognizer = RecognizerModel.fromPrimitiveType(element);

    if (recognizer != null) {
      return recognizer;
    }

    recognizer = RecognizerModel.fromStdClass(element, context);

    if (recognizer != null) {
      return recognizer;
    }

    return context.getRecognizerFactory().lookup(element.asType());
  }

  private static RecognizerModel fromStdClass(VariableElement element, ScopedContext context) {
    ProcessingEnvironment processingEnvironment = context.getProcessingContext().getProcessingEnvironment();

    if (isSubType(processingEnvironment, element, Collection.class)) {
      return ListRecognizerModel.from(element, context);
    }

//    if (isSubType(processingEnvironment, element, Map.class)) {
//      return ClassRecognizerModel.map(element, context);
//    }

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

  public  Object defaultValue() {
    return null;
  }

  public RecognizerModel map(Function<RecognizerModel, RecognizerModel> with) {
    return with.apply(this);
  }
}
