package ai.swim.structure.processor.recognizer;

import ai.swim.structure.processor.context.ScopedContext;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.Collection;
import java.util.Map;

import static ai.swim.structure.processor.Utils.isSubType;

public abstract class RecognizerModel {

  public static RecognizerModel from(TypeMirror typeMirror, ScopedContext context) {
    RecognizerModel recognizer = RecognizerModel.fromPrimitiveType(typeMirror);

    if (recognizer != null) {
      return recognizer;
    }

    recognizer = RecognizerModel.fromStdType(typeMirror, context);

    if (recognizer != null) {
      return recognizer;
    }

    recognizer = context.getRecognizerFactory().lookup(typeMirror);

    if (recognizer != null) {
      return recognizer;
    }

    switch (typeMirror.getKind()) {
      case DECLARED:
        DeclaredType declaredType = (DeclaredType) typeMirror;

        if (declaredType.getTypeArguments().isEmpty()) {
          return RecognizerReference.lookupAny(typeMirror);
        } else {
          ProcessingEnvironment processingEnvironment = context.getProcessingContext().getProcessingEnvironment();
          TypeMirror erasedType = processingEnvironment.getTypeUtils().erasure(typeMirror);

          return RecognizerReference.lookupGeneric(typeMirror, erasedType.toString(), typeMirror.toString());
        }
      case TYPEVAR:
        return RecognizerReference.untyped(typeMirror);
      case WILDCARD:
        throw new AssertionError("Recognizer model wildcard type");
      default:
        // We're out of options now. The recognizer isn't available to us now, so we'll have to hope that it's been
        // registered with the recognizer proxy for a runtime lookup.
        return RecognizerReference.lookupAny(typeMirror);
    }
  }

  private static RecognizerModel fromStdType(TypeMirror mirror, ScopedContext context) {
    ProcessingEnvironment processingEnvironment = context.getProcessingEnvironment();

    if (isSubType(processingEnvironment, mirror, Collection.class)) {
      return ListRecognizerModel.from(mirror, context);
    }

    if (isSubType(processingEnvironment, mirror, Map.class)) {
      return MapRecognizerModel.from(mirror, context);
    }

    return null;
  }

  public static RecognizerModel fromPrimitiveType(TypeMirror mirror) {
    TypeKind kind = mirror.getKind();
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

  public abstract TypeMirror type(ProcessingEnvironment environment);

  public abstract RecognizerModel retyped(ScopedContext context);
}
