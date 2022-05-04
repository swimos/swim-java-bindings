package ai.swim.structure.processor.structure.recognizer;

public class RecognizerReference extends RecognizerModel {

  public static final RecognizerModel BOXED_INTEGER = new RecognizerReference("ai.swim.structure.recognizer.primitive.IntegerRecognizer.BOXED");

  private final String initializer;

  public RecognizerReference(String initializer) {
    this.initializer = initializer;
  }

  @Override
  public String initializer() {
    return this.initializer;
  }
}
