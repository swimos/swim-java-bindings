package ai.swim.codec.data;

import org.junit.jupiter.api.Test;
import java.nio.ByteBuffer;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ByteBufferReaderTest {

  private static ByteBufferReader reader() {
    ByteBuffer buffer = ByteBuffer.allocate(128);

    buffer.putInt(10);
    buffer.putInt(20);
    buffer.putInt(30);
    buffer.putInt(40);
    buffer.putInt(50);

    int position = buffer.position();
    buffer.position(0);
    buffer.limit(position);

    return ReadBuffer.byteBuffer(buffer);
  }

  @Test
  void intOperations() {
    ByteBufferReader reader = reader();

    assertEquals(10, reader.peekInteger());
    assertEquals(0, reader.readPointer());
    assertEquals(20, reader.remaining());
    reader.advance(4);
    assertEquals(4, reader.readPointer());
    assertEquals(16, reader.remaining());

    assertEquals(20, reader.getInteger());
    assertEquals(30, reader.getInteger());
    assertEquals(40, reader.getInteger());
    assertEquals(50, reader.getInteger());

    assertTrue(reader.isEmpty());
    assertEquals(0, reader.remaining());
    assertEquals(20, reader.readPointer());
  }

  @Test
  void splitTo() {
    ReadBuffer reader = ReadBuffer.byteBuffer(ByteBuffer.wrap(new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9}));

    assertEquals(0, reader.getByte());
    assertEquals(1, reader.getByte());
    assertEquals(2, reader.getByte());

    assertEquals(7, reader.remaining());

    ReadBuffer start = reader.splitTo(4);

    assertArrayEquals(new byte[] {3, 4, 5, 6}, start.peekArray());
    assertArrayEquals(new byte[] {7, 8, 9}, reader.peekArray());

    start.advance(4);
    assertEquals(0, start.remaining());

    reader.advance(3);
    assertEquals(0, reader.remaining());

    assertThrows(BufferOverflowException.class, () -> start.advance(10));
    assertThrows(BufferOverflowException.class, () -> reader.advance(10));
  }

}