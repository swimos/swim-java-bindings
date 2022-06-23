package ai.swim.structure.processor.recognizer;

import javax.lang.model.type.TypeMirror;

public class RecognizerReference extends RecognizerModel {

  private final String initializer;

  private RecognizerReference(String initializer) {
    this.initializer = initializer;
  }

  public static RecognizerModel lookupAny(TypeMirror mirror) {
    return new RecognizerReference(String.format("ai.swim.structure.recognizer.proxy.RecognizerProxy.getInstance().lookup(%s.class)", mirror.toString()));
  }

  public static StructuralRecognizer.Reference lookupStructural(TypeMirror mirror) {
    return new StructuralRecognizer.Reference(mirror);
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

  public static class Formatter {
    private final String packageName;

    public Formatter(String packageName) {
      this.packageName = packageName;
    }

    public RecognizerReference recognizerFor(String name) {
      return new RecognizerReference(String.format("%s.%s", this.packageName, name));
    }
  }
}

