package ai.swim.server.codec;

import ai.swim.codec.Size;
import ai.swim.codec.data.ByteWriter;
import ai.swim.codec.encoder.Encoder;

/**
 * Encodes a byte array into {@link ByteWriter} prefixed by the length of the byte array.
 */
public class WithLengthBytesEncoder implements Encoder<byte[]> {
  @Override
  public void encode(byte[] target, ByteWriter dst) {
    dst.reserve(Size.LONG + target.length);
    dst.writeLong(target.length);
    dst.writeByteArray(target);
  }
}
