package ai.swim.structure.recognizer.std.collections;

import ai.swim.recon.event.ReadEvent;
import ai.swim.structure.recognizer.std.ScalarRecognizer;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static ai.swim.structure.RecognizerTestUtil.runTest;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CollectionRecognizerTest {

  private static List<ReadEvent> events() {
    return List.of(
      ReadEvent.startBody(),
      ReadEvent.number(1),
      ReadEvent.number(2),
      ReadEvent.number(3),
      ReadEvent.endRecord()
    );
  }

  private static <T, E extends Collection<T>, O> void runCollectionTest(CollectionRecognizer<T, E, O> recognizer, O expected) {
    O collection = runTest(recognizer, events());
    assertEquals(collection, expected);
  }

  @Test
  void testList() {
    runCollectionTest(new ListRecognizer<>(ScalarRecognizer.INTEGER, false), List.of(1, 2, 3));
  }

  @Test
  void testHashSet() {
    runCollectionTest(new HashSetRecognizer<>(ScalarRecognizer.INTEGER, false), new HashSet<>(Set.of(1, 2, 3)));
  }

  @Test
  void testArray() {
    Integer[] actual = runTest(new ArrayRecognizer<>(Integer.class, ScalarRecognizer.INTEGER), events());
    assertArrayEquals(actual, new Integer[]{1, 2, 3});
  }

}