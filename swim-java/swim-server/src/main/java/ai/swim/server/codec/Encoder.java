package ai.swim.server.codec;

import java.nio.ByteBuffer;

public interface Encoder<T> {

  void encode(T target, Bytes buffer);

}
