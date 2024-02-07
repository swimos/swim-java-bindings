package ai.swim.codec.input;

import ai.swim.codec.data.ByteReader;
import ai.swim.codec.data.ByteWriter;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReadBufferInputTest {

  @Test
  void has() {
    ByteWriter writer = new ByteWriter();
    writer.writeByteArray(new byte[] {1, 2, 3, 4, 5});

    Input input = Input.readBuffer(writer.reader());

    for (int i = 4; i > 0; i--) {
      assertTrue(input.has(i));
      input.step();
    }
  }

  @Test
  void testBounded() {
    ByteWriter bytes = new ByteWriter();
    bytes.writeByteArray(new byte[] {1, 2, 3, 4, 5, 7, 8, 9});

    ByteReader reader = bytes.reader();
    reader.advance(2);

    Input input = new ReadBufferInput(reader, false, 4);

    assertEquals(3, input.head());
    assertTrue(input.has(1));
    assertFalse(input.has(3));

    input = input.step();
    assertEquals(4, input.head());
    assertTrue(input.isContinuation());

    input = input.step();
    assertEquals(5, input.head());
    assertFalse(input.isContinuation());

    input = input.step();
    assertTrue(input.isDone());
  }

  @Test
  void testUnbounded() {
    ByteWriter bytes = new ByteWriter();
    bytes.writeByteArray(new byte[] {1, 2, 3});

    Input input = Input.readBuffer(bytes.reader());

    assertTrue(input.has(1));
    assertFalse(input.has(4));
    assertEquals(1, input.head());

    input = input.step();
    assertTrue(input.isContinuation());
    assertEquals(2, input.head());

    input = input.step();
    assertFalse(input.isContinuation());
    assertEquals(3, input.head());

    input = input.step();
    assertTrue(input.isDone());
  }

}