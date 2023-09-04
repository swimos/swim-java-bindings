package ai.swim.server.codec;

import ai.swim.codec.Size;
import ai.swim.codec.data.ByteReader;
import ai.swim.codec.decoder.Decoder;
import ai.swim.codec.decoder.DecoderException;

/**
 * A decoder that decodes N bytes that are prefixed by their len.
 */
public class WithLengthBytesDecoder extends Decoder<ByteReader> {

  @Override
  public Decoder<ByteReader> decode(ByteReader buffer) throws DecoderException {
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
        throw new DecoderException(e);
      }
    }
  }

  @Override
  public Decoder<ByteReader> reset() {
    return this;
  }

}
