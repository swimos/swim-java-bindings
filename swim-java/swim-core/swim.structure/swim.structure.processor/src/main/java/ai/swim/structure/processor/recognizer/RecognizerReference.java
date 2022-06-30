package ai.swim.structure.processor.recognizer;

import ai.swim.structure.processor.context.NameFactory;
import ai.swim.structure.processor.context.ScopedContext;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

public class RecognizerReference extends RecognizerModel {

  protected final String initializer;
  protected final TypeMirror mirror;

  private RecognizerReference(TypeMirror mirror, String initializer) {
    this.mirror = mirror;
    this.initializer = initializer;
  }

  public static RecognizerModel lookupAny(TypeMirror mirror) {
    return new RecognizerReference(mirror, String.format("ai.swim.structure.recognizer.proxy.RecognizerProxy.getInstance().lookup(%s.class)", mirror.toString()));
  }

  public static RecognizerModel untyped(TypeMirror objectMirror) {
    return new UntypedRecognizerReference(objectMirror, "new ai.swim.structure.recognizer.untyped.UntypedRecognizer()");
  }

  public static StructuralRecognizer.Reference lookupStructural(TypeMirror mirror) {
    return new StructuralRecognizer.Reference(mirror);
  }

  public static RecognizerModel lookupGeneric(TypeMirror mirror, String qualifiedName, String className) {
    String cast = String.format("(Class<%s>)(Class<?>)", className);
    return new RecognizerReference(mirror, String.format("ai.swim.structure.recognizer.proxy.RecognizerProxy.getInstance().lookup(%s %s.class)", cast, qualifiedName));
  }

  @Override
  public String recognizerInitializer() {
    return this.initializer;
  }

  @Override
  public TypeMirror type(ProcessingEnvironment environment) {
    return this.mirror;
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

  public static class Formatter {
    private final String packageName;

    public Formatter(String packageName) {
      this.packageName = packageName;
    }

    public RecognizerReference recognizerFor(String name) {
      return new RecognizerReference(null, String.format("%s.%s", this.packageName, name));
    }
  }

  static class UntypedRecognizerReference extends RecognizerReference {
    private UntypedRecognizerReference(TypeMirror mirror, String initializer) {
      super(mirror, initializer);
    }

    @Override
    public RecognizerModel retyped(ScopedContext context) {
      NameFactory nameFactory = context.getNameFactory();

      switch (mirror.getKind()){
        case TYPEVAR:
          TypeVariable typeVariable = (TypeVariable) mirror;
          return new RecognizerReference(mirror, String.format("%s.build()",nameFactory.typeParameterName(typeVariable.toString())));
        case DECLARED:
          System.out.println("Untyped: declared");
          break;
        default:
          System.out.println("Untyped: " + mirror.getKind());
          break;
      }

      throw new AssertionError();
    }
  }
}
