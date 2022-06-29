package ai.swim.structure.processor.recognizer;

import javax.lang.model.type.TypeMirror;

public class RecognizerInstance extends StructuralRecognizer {

  private final String type;

  public RecognizerInstance(String type) {
    this.type = type;
  }

  @Override
  public String recognizerInitializer() {
    return String.format("new %s()", type);
  }

  @Override
  public TypeMirror type() {
    return null;
  }

}
