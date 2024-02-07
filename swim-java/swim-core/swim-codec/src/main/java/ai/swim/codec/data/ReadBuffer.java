package ai.swim.codec.data;

import java.nio.ByteBuffer;

public interface ReadBuffer {
  static ByteBufferReader byteBuffer(ByteBuffer buffer) {
    return new ByteBufferReader(buffer);
  }

  static ByteReader fromArray(byte[] array) {
    return ByteReader.fromArray(array);
  }

  void advance(int by);

  int remaining();

  default boolean isEmpty() {
    return remaining() == 0;
  }

  int readPointer();

  byte getByte();

  byte peekByte();

  byte peekByte(int offset);

  int getInteger();

  int peekInteger();

  long getLong();

  long peekLong();

  byte[] getArray();

  byte[] peekArray();

  int getByteArray(byte[] into);

  ReadBuffer splitTo(int at);

  ReadBuffer splitOff(int at);

  void unsplit(ReadBuffer from);

  ReadBuffer clone();

}
