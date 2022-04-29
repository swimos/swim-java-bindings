package ai.swim.structure.recognizer;

import ai.swim.recon.event.ReadEvent;
import ai.swim.recon.event.ReadNumberValue;

public class ObjectRecognizer extends Recognizer<Object> {
  @Override
  public Recognizer<Object> feedEvent(ReadEvent event) {
    if (event.isNumber()) {
      return Recognizer.done(((ReadNumberValue) event).value());
    }

    throw new AssertionError();
  }

  @Override
  public Recognizer<Object> reset() {
    return null;
  }
}
