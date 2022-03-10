package ai.swim.structure.form.recognizer.primitive;

import ai.swim.structure.form.event.ReadEvent;
import ai.swim.structure.form.event.ReadNumberValue;
import ai.swim.structure.form.recognizer.Recognizer;

public class IntegerRecognizer extends Recognizer<Integer> {

  @Override
  public Recognizer<Integer> feedEvent(ReadEvent event) {
    if (event.isNumber()) {
      ReadNumberValue readNumberValueEvent = (ReadNumberValue) event;
      return Recognizer.done(readNumberValueEvent.value().intValue());
    } else {
      return Recognizer.error(new RuntimeException("Expected an integer"));
    }
  }

}
