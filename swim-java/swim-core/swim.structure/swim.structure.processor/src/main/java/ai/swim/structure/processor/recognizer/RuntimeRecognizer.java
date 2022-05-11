package ai.swim.structure.processor.recognizer;

public class RuntimeRecognizer extends RecognizerModel {

  private final String type;

  public RuntimeRecognizer(String type) {
    this.type = type;
  }

  @Override
  public String recognizerInitializer() {
    return String.format("%s.class", type);
  }

}
