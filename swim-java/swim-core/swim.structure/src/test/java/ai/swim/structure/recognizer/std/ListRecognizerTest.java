package ai.swim.structure.recognizer.std;

import ai.swim.recon.event.ReadEvent;
import ai.swim.structure.RecognizerTestUtil;
import ai.swim.structure.recognizer.Recognizer;
import org.junit.jupiter.api.Test;

import java.util.List;

class ListRecognizerTest {
  @Test
  void testRecognizer() {
    Recognizer<List<Integer>> recognizer = new ListRecognizer<>(ScalarRecognizer.INTEGER, false);
    List<ReadEvent> events = List.of(
        ReadEvent.startBody(),
        ReadEvent.number(1),
        ReadEvent.number(2),
        ReadEvent.number(3),
        ReadEvent.endRecord()
    );

    RecognizerTestUtil.runTest(recognizer, events);
  }
}