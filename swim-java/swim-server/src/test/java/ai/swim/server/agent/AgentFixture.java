package ai.swim.server.agent;

import ai.swim.codec.data.ByteWriter;
import ai.swim.codec.encoder.Encoder;
import java.nio.charset.StandardCharsets;

public class AgentFixture {

  public static void writeInt(int v, ByteWriter into) {
    byte[] bytes = Integer.toString(v).getBytes(StandardCharsets.UTF_8);
    into.writeLong(bytes.length);
    into.writeByteArray(bytes);
  }

  public static <E> ByteWriter encodeIter(Iterable<E> iterator, Encoder<E> encoder) {
    ByteWriter requestBytes = new ByteWriter();

    for (E e : iterator) {
      encoder.encode(e, requestBytes);
    }

    return requestBytes;
  }

}
