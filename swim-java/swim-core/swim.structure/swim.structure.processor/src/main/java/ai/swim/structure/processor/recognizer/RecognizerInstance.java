package ai.swim.structure.processor.recognizer;

public class RecognizerInstance extends StructuralRecognizer {

  private final String type;

  public RecognizerInstance(String type) {
    this.type = type;
  }

  @Override
  public String recognizerInitializer() {
    return String.format("new %s()", type);
  }

}
