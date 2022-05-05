package ai.swim.structure.recognizer;

import ai.swim.recon.event.ReadEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RecognizerRequiredTest {
  @Test
  void testRequired() {
    Recognizer<Integer> integerRecognizer = ScalarRecognizer.BOXED_INTEGER.required();
    integerRecognizer = integerRecognizer.feedEvent(ReadEvent.extant());

    assertTrue(integerRecognizer.isError());
  }
}