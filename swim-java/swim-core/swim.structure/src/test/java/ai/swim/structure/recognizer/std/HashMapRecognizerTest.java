package ai.swim.structure.recognizer.std;

import ai.swim.recon.event.ReadEvent;
import ai.swim.structure.recognizer.Recognizer;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;

import static ai.swim.structure.RecognizerTestUtil.runTest;

class HashMapRecognizerTest {

  @Test
  void testHashMap() {
    Recognizer<HashMap<String, Integer>> recognizer = new HashMapRecognizer<>(ScalarRecognizer.STRING, ScalarRecognizer.INTEGER);
    List<ReadEvent> events = List.of(
        ReadEvent.startBody(),
        ReadEvent.text("a"),
        ReadEvent.slot(),
        ReadEvent.number(1),
        ReadEvent.text("b"),
        ReadEvent.slot(),
        ReadEvent.number(2),
        ReadEvent.text("c"),
        ReadEvent.slot(),
        ReadEvent.number(3),
        ReadEvent.endRecord()
    );

    runTest(recognizer, events);
  }

}