package ai.swim.server.codec;

public class WithLengthBytesEncoder implements Encoder<byte[]> {
  @Override
  public void encode(byte[] target, Bytes dst) {
    dst.reserve(Size.LONG + target.length);
    dst.writeLong(target.length);
    dst.writeByteArray(target);
  }
}
