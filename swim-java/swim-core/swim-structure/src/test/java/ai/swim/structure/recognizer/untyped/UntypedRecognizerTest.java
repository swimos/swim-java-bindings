package ai.swim.structure.recognizer.untyped;

import ai.swim.recon.event.ReadEvent;
import ai.swim.structure.recognizer.Recognizer;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UntypedRecognizerTest {

  void singleEventOk(ReadEvent event, Object expected) {
    Recognizer<?> recognizer = new UntypedRecognizer<>();
    recognizer = recognizer.feedEvent(event);

    assertTrue(recognizer.isDone());
    assertEquals(recognizer.bind(), expected);
  }

  @Test
  void readsScalars() {
    Recognizer<?> recognizer = new UntypedRecognizer<>();
    recognizer = recognizer.feedEvent(ReadEvent.blob(new byte[] {1, 2, 3}));

    assertTrue(recognizer.isDone());
    assertArrayEquals((byte[]) recognizer.bind(), new byte[] {1, 2, 3});

    singleEventOk(ReadEvent.bool(true), true);
    singleEventOk(ReadEvent.number(1), 1);
    singleEventOk(ReadEvent.number(1L), 1L);
    singleEventOk(ReadEvent.number(1f), 1f);
    singleEventOk(ReadEvent.number(1d), 1d);
    singleEventOk(ReadEvent.number(BigInteger.TEN), BigInteger.TEN);
    singleEventOk(ReadEvent.number(BigDecimal.TEN), BigDecimal.TEN);
    singleEventOk(ReadEvent.text("hello"), "hello");
  }

  void multipleEventsOk(List<ReadEvent> events, Object expected) {
    Recognizer<?> recognizer = new UntypedRecognizer<>();

    for (int i = 0; i < events.size(); i++) {
      ReadEvent event = events.get(i);
      boolean isDone = i + 1 == events.size();
      recognizer = recognizer.feedEvent(event);

      if (isDone) {
        assertTrue(recognizer.isDone());
        assertEquals(recognizer.bind(), expected);
      } else {
        assertTrue(recognizer.isCont());
      }
    }
  }

  @Test
  void readsEmptyList() {
    List<ReadEvent> events = List.of(
        ReadEvent.startBody(),
        ReadEvent.endRecord()
                                    );

    multipleEventsOk(events, Collections.emptyList());
  }

  @Test
  void readsSimpleList1() {
    List<ReadEvent> events = List.of(
        ReadEvent.startBody(),
        ReadEvent.number(1),
        ReadEvent.endRecord()
                                    );

    multipleEventsOk(events, List.of(1));
  }

  @Test
  void readsSimpleList2() {
    List<ReadEvent> events = List.of(
        ReadEvent.startBody(),
        ReadEvent.number(1),
        ReadEvent.number(2),
        ReadEvent.number(3),
        ReadEvent.endRecord()
                                    );

    multipleEventsOk(events, List.of(1, 2, 3));
  }

  @Test
  void readsSimpleMap1() {
    List<ReadEvent> events = List.of(
        ReadEvent.startBody(),
        ReadEvent.number(1),
        ReadEvent.slot(),
        ReadEvent.number(1),
        ReadEvent.endRecord()
                                    );

    multipleEventsOk(events, Map.of(1, 1));
  }

  @Test
  void readsSimpleMap2() {
    List<ReadEvent> events = List.of(
        ReadEvent.startBody(),
        ReadEvent.number(1),
        ReadEvent.slot(),
        ReadEvent.number(1),
        ReadEvent.number(2),
        ReadEvent.slot(),
        ReadEvent.number(2),
        ReadEvent.endRecord()
                                    );

    multipleEventsOk(events, Map.of(1, 1, 2, 2));
  }

  @Test
  void readsListOfMaps() {
    List<ReadEvent> events = List.of(
        ReadEvent.startBody(),

        ReadEvent.startBody(),
        ReadEvent.number(1),
        ReadEvent.slot(),
        ReadEvent.number(1),
        ReadEvent.endRecord(),

        ReadEvent.startBody(),
        ReadEvent.number(2),
        ReadEvent.slot(),
        ReadEvent.number(2),
        ReadEvent.endRecord(),

        ReadEvent.startBody(),
        ReadEvent.number(3),
        ReadEvent.slot(),
        ReadEvent.number(3),
        ReadEvent.endRecord(),

        ReadEvent.endRecord()
                                    );
    multipleEventsOk(events, List.of(
        Map.of(1, 1),
        Map.of(2, 2),
        Map.of(3, 3)
                                    ));
  }

  @Test
  void readsMapOfLists() {
    List<ReadEvent> events = List.of(
        ReadEvent.startBody(),

        ReadEvent.number(1),
        ReadEvent.slot(),
        ReadEvent.startBody(),
        ReadEvent.number(1),
        ReadEvent.number(2),
        ReadEvent.number(3),
        ReadEvent.endRecord(),

        ReadEvent.number(2),
        ReadEvent.slot(),
        ReadEvent.startBody(),
        ReadEvent.number(4),
        ReadEvent.number(5),
        ReadEvent.number(6),
        ReadEvent.endRecord(),

        ReadEvent.number(3),
        ReadEvent.slot(),
        ReadEvent.startBody(),
        ReadEvent.number(7),
        ReadEvent.number(8),
        ReadEvent.number(9),
        ReadEvent.endRecord(),

        ReadEvent.endRecord()
                                    );

    multipleEventsOk(events, Map.of(
        1, List.of(1, 2, 3),
        2, List.of(4, 5, 6),
        3, List.of(7, 8, 9)
                                   ));
  }

  void testFail(List<ReadEvent> events) {
    Recognizer<?> recognizer = new UntypedRecognizer<>();

    for (ReadEvent event : events) {
      recognizer = recognizer.feedEvent(event);
      if (recognizer.isError()) {
        return;
      }
    }

    assertTrue(recognizer.isError());
  }

  @Test
  void badStructures() {
    testFail(List.of(
        ReadEvent.startBody(),
        ReadEvent.slot()
                    ));
    testFail(List.of(ReadEvent.startAttribute("bad")));
    testFail(List.of(ReadEvent.endRecord()));
    testFail(List.of(
        ReadEvent.startBody(),
        ReadEvent.number(1),
        ReadEvent.slot(),
        ReadEvent.number(1),
        ReadEvent.number(2),
        ReadEvent.slot(),
        ReadEvent.slot()
                    ));
  }
}