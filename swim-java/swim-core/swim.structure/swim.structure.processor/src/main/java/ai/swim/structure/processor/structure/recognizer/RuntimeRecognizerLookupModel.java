package ai.swim.structure.processor.structure.recognizer;

public class RuntimeRecognizerLookupModel extends RecognizerModel {

  private final String type;

  public RuntimeRecognizerLookupModel(String type) {
    this.type = type;
  }

  @Override
  public String initializer() {
    return String.format("%s.class", type);
  }

}
