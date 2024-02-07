package ai.swim.server.buffer;

public interface ByteWriter {
  void writeByte(byte b);

  void writeByte(byte b, int at);

  void writeInteger(int b);

  void writeInteger(int b, int startAt);

  void writeLong(long l);

  void writeLong(long l, int startAt);

  int remaining();
}
