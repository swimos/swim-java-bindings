package ai.swim.structure.recognizer.primitive;

import ai.swim.recon.event.ReadEvent;
import ai.swim.recon.event.ReadNumberValue;
import ai.swim.structure.recognizer.Recognizer;

public class IntegerRecognizer extends Recognizer<Integer> {

  public static final Recognizer<Integer> INSTANCE = new IntegerRecognizer();

  private IntegerRecognizer(){}

  @Override
  public Recognizer<Integer> feedEvent(ReadEvent event) {
    if (event.isNumber()) {
      ReadNumberValue readNumberValueEvent = (ReadNumberValue) event;
      return Recognizer.done(readNumberValueEvent.value().intValue());
    } else {
      return Recognizer.error(new RuntimeException("Expected an integer"));
    }
  }

  @Override
  public Recognizer<Integer> reset() {
    return INSTANCE;
  }

}
