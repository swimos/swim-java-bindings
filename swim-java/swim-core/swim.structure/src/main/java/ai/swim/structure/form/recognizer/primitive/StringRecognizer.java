package ai.swim.structure.form.recognizer.primitive;

import ai.swim.structure.form.event.ReadEvent;
import ai.swim.structure.form.event.ReadTextValue;
import ai.swim.structure.form.recognizer.Recognizer;

public class StringRecognizer extends Recognizer<String> {
  @Override
  public Recognizer<String> feedEvent(ReadEvent event) {
    if (event.isText()) {
      ReadTextValue readTextValue = (ReadTextValue) event;
      return Recognizer.done(readTextValue.value());
    } else {
      return Recognizer.error(new RuntimeException("todo"));
    }
  }
}
