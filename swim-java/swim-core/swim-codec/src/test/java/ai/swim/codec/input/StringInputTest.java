package ai.swim.codec.input;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StringInputTest {

  @Test
  void testTake() {
    Input input = Input.string("abcd");
    assertEquals("ab", StringInput.codePointsToString(input.take(2)));
  }

  @Test
  void testHas() {
    Input input = Input.string("abcde");

    assertTrue(input.has(0));
    assertTrue(input.has(1));
    assertTrue(input.has(2));
    assertTrue(input.has(3));
    assertTrue(input.has(4));
    assertTrue(input.has(5));

    assertFalse(input.has(6));
  }

}