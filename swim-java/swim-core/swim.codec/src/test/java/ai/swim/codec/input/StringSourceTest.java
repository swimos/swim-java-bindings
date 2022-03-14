package ai.swim.codec.input;

import ai.swim.codec.source.Source;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StringSourceTest {

  @Test
  void testTake() {
    Source source = Source.string("abcd");

    assertEquals("ab", new String(source.take(2)));
  }

  @Test
  void testHas() {
    Source source = Source.string("abcde");

    assertTrue(source.has(0));
    assertTrue(source.has(1));
    assertTrue(source.has(2));
    assertTrue(source.has(3));
    assertTrue(source.has(4));
    assertTrue(source.has(5));

    assertFalse(source.has(6));
  }

}