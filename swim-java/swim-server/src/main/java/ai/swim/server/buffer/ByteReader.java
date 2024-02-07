package ai.swim.server.buffer;

public interface ByteReader {
  byte getByte();

  byte peekByte();

  byte peekByte(int offset);

  int getInt();

  int peekInt();

  long getLong();

  long peekLong();

  void advance(int by);

  int remaining();
}
