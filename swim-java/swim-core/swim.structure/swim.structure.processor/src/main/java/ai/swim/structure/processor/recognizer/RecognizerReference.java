package ai.swim.structure.processor.recognizer;

import javax.lang.model.type.TypeMirror;

public class RecognizerReference extends RecognizerModel {

  private final String initializer;
  private final TypeMirror mirror;

  private RecognizerReference(TypeMirror mirror, String initializer) {
    this.mirror = mirror;
    this.initializer = initializer;
  }

  public static RecognizerModel lookupAny(TypeMirror mirror) {
    return new RecognizerReference(mirror,String.format("ai.swim.structure.recognizer.proxy.RecognizerProxy.getInstance().lookup(%s.class)", mirror.toString()));
  }

  public static RecognizerModel untyped(TypeMirror objectMirror) {
    return new RecognizerReference(objectMirror, "new ai.swim.structure.recognizer.untyped.UntypedRecognizer()");
  }

  public static StructuralRecognizer.Reference lookupStructural(TypeMirror mirror) {
    return new StructuralRecognizer.Reference(mirror);
  }

  public static RecognizerModel lookupGeneric(TypeMirror mirror,String qualifiedName, String className) {
    String cast = String.format("(Class<%s>)(Class<?>)", className);
    return new RecognizerReference(mirror, String.format("ai.swim.structure.recognizer.proxy.RecognizerProxy.getInstance().lookup(%s %s.class)", cast, qualifiedName));
  }

  @Override
  public String recognizerInitializer() {
    return this.initializer;
  }

  @Override
  public TypeMirror type() {
    return this.mirror;
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
      return new RecognizerReference(null, String.format("%s.%s", this.packageName, name));
    }
  }
}

