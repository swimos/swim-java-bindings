package ai.swim.structure.recognizer;

import ai.swim.recon.event.ReadEvent;
import ai.swim.structure.recognizer.std.ScalarRecognizer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecognizerRequiredTest {
  @Test
  void testRequired() {
    Recognizer<Integer> integerRecognizer = ScalarRecognizer.INTEGER.required();
    integerRecognizer = integerRecognizer.feedEvent(ReadEvent.extant());

    assertTrue(integerRecognizer.isError());
  }
}