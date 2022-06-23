package ai.swim.structure.processor.recognizer;

import javax.lang.model.type.TypeMirror;

public abstract class StructuralRecognizer extends RecognizerModel {
  public static class Reference extends StructuralRecognizer {
    private final String initializer;

    Reference(TypeMirror initializer) {
      this.initializer = String.format("ai.swim.structure.recognizer.proxy.RecognizerProxy.getInstance().lookupStructural(%s.class)", initializer.toString());
    }

    @Override
    public String recognizerInitializer() {
      return this.initializer;
    }

    @Override
    public String toString() {
      return "RecognizerReference{" +
          "initializer='" + initializer + '\'' +
          '}';
    }
  }
}
