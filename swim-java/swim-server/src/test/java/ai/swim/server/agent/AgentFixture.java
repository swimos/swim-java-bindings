package ai.swim.server.agent;

import ai.swim.codec.data.ByteWriter;
import ai.swim.codec.encoder.Encoder;
import java.nio.charset.StandardCharsets;

public class AgentFixture {

  public static void writeIntString(int v, ByteWriter into) {
    byte[] bytes = Integer.toString(v).getBytes(StandardCharsets.UTF_8);
    into.writeLong(bytes.length);
    into.writeByteArray(bytes);
  }

  public static void writeBooleanString(Boolean v, ByteWriter into) {
    byte[] bytes = Boolean.toString(v).getBytes(StandardCharsets.UTF_8);
    into.writeLong(bytes.length);
    into.writeByteArray(bytes);
  }

  public static <E> ByteWriter encodeIter(Iterable<E> iterator, Encoder<E> encoder) {
    ByteWriter writer = new ByteWriter();
    encodeIter(writer, iterator, encoder);
    return writer;
  }

  public static <E> void encodeIter(ByteWriter writer, Iterable<E> iterator, Encoder<E> encoder) {
    for (E e : iterator) {
      encoder.encode(e, writer);
    }
  }

}
