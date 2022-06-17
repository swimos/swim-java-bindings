package ai.swim.structure.recognizer.std;

import ai.swim.recon.event.ReadEvent;
import ai.swim.structure.recognizer.Recognizer;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScalarRecognizerTest {

  <T> void runTestOk(Recognizer<T> recognizer, T expected) {
    assertTrue(recognizer.isDone());
    assertEquals(recognizer.bind(), expected);
  }

  @Test
  void testInt() {
    runTestOk(ScalarRecognizer.INTEGER.feedEvent(ReadEvent.number(1)), 1);
    runTestOk(ScalarRecognizer.INTEGER.feedEvent(ReadEvent.number(Integer.MAX_VALUE)), Integer.MAX_VALUE);
    runTestOk(ScalarRecognizer.INTEGER.feedEvent(ReadEvent.number(Long.valueOf(123))), 123);
    runTestOk(ScalarRecognizer.INTEGER.feedEvent(ReadEvent.number(BigInteger.TEN)), 10);
  }

  @Test
  void testLong() {
    runTestOk(ScalarRecognizer.LONG.feedEvent(ReadEvent.number(1L)), 1L);
    runTestOk(ScalarRecognizer.LONG.feedEvent(ReadEvent.number(Long.MAX_VALUE)), Long.MAX_VALUE);
    runTestOk(ScalarRecognizer.LONG.feedEvent(ReadEvent.number(123)), 123L);
    runTestOk(ScalarRecognizer.LONG.feedEvent(ReadEvent.number(BigInteger.TEN)), 10L);
  }

  @Test
  void testFloat() {
    runTestOk(ScalarRecognizer.FLOAT.feedEvent(ReadEvent.number(1L)), 1f);
    runTestOk(ScalarRecognizer.FLOAT.feedEvent(ReadEvent.number(100d)), 100f);
    runTestOk(ScalarRecognizer.FLOAT.feedEvent(ReadEvent.number(123.456d)), 123.456f);
    runTestOk(ScalarRecognizer.FLOAT.feedEvent(ReadEvent.number(BigInteger.TEN)), 10f);
  }
}