package ai.swim.structure.processor.structure.recognizer;

public class RuntimeRecognizerLookupModel extends RecognizerModel {
  private final static RecognizerModel instance = new RuntimeRecognizerLookupModel();

  @Override
  public String initializer() {
    throw new AssertionError("RuntimeRecognizerLookupModel");
  }

  public static RecognizerModel instance() {
    return instance;
  }

}
