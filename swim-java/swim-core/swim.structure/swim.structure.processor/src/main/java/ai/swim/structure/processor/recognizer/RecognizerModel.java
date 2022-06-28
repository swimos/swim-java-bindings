package ai.swim.structure.processor.recognizer;

import ai.swim.structure.processor.context.ScopedContext;
import com.sun.jdi.ClassType;
import org.checkerframework.checker.units.qual.C;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.*;
import java.util.Collection;
import java.util.List;
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

    switch (element.asType().getKind()) {
      case DECLARED:
        DeclaredType declaredType = (DeclaredType) element.asType();

        if (declaredType.getTypeArguments().isEmpty()) {
          return RecognizerReference.lookupAny(element.asType());
        } else {
          ProcessingEnvironment processingEnvironment = context.getProcessingContext().getProcessingEnvironment();
          TypeMirror erasedType = processingEnvironment.getTypeUtils().erasure(element.asType());

          return RecognizerReference.lookupGeneric(erasedType.toString(), element.asType().toString());
        }
      case TYPEVAR:
        return RecognizerReference.untyped();
      default:
        // We're out of options now. The recognizer isn't available to us now, so we'll have to hope that it's been
        // registered with the recognizer proxy for a runtime lookup, or it will be derived at runtime and incur the penalty
        // of reflection.
        return RecognizerReference.lookupAny(element.asType());
    }
  }

  private static RecognizerModel fromTypeVar

  private static RecognizerModel fromStdType(Element element, ScopedContext context) {
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();

    if (isSubType(processingEnvironment, element, Collection.class)) {
      return ListRecognizerModel.from(element, context);
    }

    if (isSubType(processingEnvironment, element, Map.class)) {
//      return MapRecognizerModel.from(element, context);
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
