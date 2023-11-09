package ai.swim.server.codec;

/**
 * A decoder that decodes N bytes that are prefixed by their len.
 */
public class WithLengthBytesDecoder extends Decoder<Bytes> {

  @Override
  public Decoder<Bytes> decode(Bytes buffer) throws DecoderException {
    if (buffer.remaining() < Size.LONG) {
      return this;
    } else {
      long len = buffer.peekLong();
      try {
        int intValue = Math.toIntExact(len);
        if (buffer.remaining() >= Size.LONG + intValue) {
          buffer.getLong();
          return Decoder.done(this, buffer.splitTo(intValue));
        } else {
          return this;
        }
      } catch (ArithmeticException e) {
        // Java array capacity is an integer but it is possible that Rust/a peer sends a long representation.
        throw new DecoderException("Integer overflow", e);
      }
    }
  }

  @Override
  public Decoder<Bytes> reset() {
    return this;
  }

}
