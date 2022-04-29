package ai.swim.structure.processor.structure.recognizer;

import ai.swim.structure.processor.structure.accessor.Accessor;
import ai.swim.structure.processor.structure.ElementSchema;

public class Recognizable {
  private final Accessor accessor;
  private final RecognizerModel recognizer;

  public Recognizable(Accessor accessor, RecognizerModel recognizer) {
    this.accessor = accessor;
    this.recognizer = recognizer;
  }

  @Override
  public String toString() {
    return "Recognizable{" +
        "accessor=" + accessor +
        ", recognizer=" + recognizer +
        '}';
  }
}
