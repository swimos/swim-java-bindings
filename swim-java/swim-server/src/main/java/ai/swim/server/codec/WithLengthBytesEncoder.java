package ai.swim.server.codec;

/**
 * Encodes a byte array into {@link Bytes} prefixed by the length of the byte array.
 */
public class WithLengthBytesEncoder implements Encoder<byte[]> {
  @Override
  public void encode(byte[] target, Bytes dst) {
    dst.reserve(Size.LONG + target.length);
    dst.writeLong(target.length);
    dst.writeByteArray(target);
  }
}
