package ai.swim.structure.recognizer.primitive;

import ai.swim.recon.event.ReadEvent;
import ai.swim.recon.event.ReadTextValue;
import ai.swim.structure.recognizer.Recognizer;

public class StringRecognizer extends Recognizer<String> {

  public static final Recognizer<String> INSTANCE = new StringRecognizer();

  private StringRecognizer(){}

  @Override
  public Recognizer<String> feedEvent(ReadEvent event) {
    if (event.isText()) {
      ReadTextValue readTextValue = (ReadTextValue) event;
      return Recognizer.done(readTextValue.value(), this);
    } else {
      return Recognizer.error(new RuntimeException("todo"));
    }
  }

  @Override
  public Recognizer<String> reset() {
    return INSTANCE;
  }

}
