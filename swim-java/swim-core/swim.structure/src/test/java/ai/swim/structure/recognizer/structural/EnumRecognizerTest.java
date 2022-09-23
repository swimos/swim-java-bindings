package ai.swim.structure.recognizer.structural;

import ai.swim.recon.event.ReadEvent;
import ai.swim.structure.recognizer.Recognizer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static ai.swim.structure.RecognizerTestUtil.runTest;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EnumRecognizerTest {

  @Test
  void readSimpleEnum() {
    Recognizer<Level> recognizer = new EnumRecognizer<>(Level.class);
    List<ReadEvent> events = List.of(
        ReadEvent.startAttribute("warn"),
        ReadEvent.endAttribute(),
        ReadEvent.startBody(),
        ReadEvent.endRecord()
    );

    Level level = runTest(recognizer, events);
    assertEquals(level, Level.Warn);
  }

  enum Level {
    Info(0),
    Warn(1),
    Error(2);

    private final int idx;

    Level(int idx) {
      this.idx = idx;
    }
  }

}
