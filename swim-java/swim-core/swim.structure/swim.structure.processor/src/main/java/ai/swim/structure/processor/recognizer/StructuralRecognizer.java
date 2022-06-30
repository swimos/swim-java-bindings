package ai.swim.structure.processor.recognizer;

import ai.swim.structure.processor.context.ScopedContext;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeMirror;

public abstract class StructuralRecognizer extends RecognizerModel {
  public static class Reference extends StructuralRecognizer {
    private final String initializer;
    private final TypeMirror type;

    Reference(TypeMirror type) {
      this.type = type;
      this.initializer = String.format("ai.swim.structure.recognizer.proxy.RecognizerProxy.getInstance().lookupStructural(%s.class)", type.toString());
    }

    @Override
    public String recognizerInitializer() {
      return this.initializer;
    }

    @Override
    public TypeMirror type(ProcessingEnvironment environment) {
      return type;
    }

    @Override
    public RecognizerModel retyped(ScopedContext context) {
      return this;
    }

    @Override
    public String toString() {
      return "RecognizerReference{" +
          "initializer='" + initializer + '\'' +
          '}';
    }
  }
}
