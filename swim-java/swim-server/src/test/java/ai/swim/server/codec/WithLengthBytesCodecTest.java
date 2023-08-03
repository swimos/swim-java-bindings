package ai.swim.server.codec;

import org.junit.jupiter.api.Test;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WithLengthBytesCodecTest {

  @Test
  void roundTrip() throws DecoderException {
    byte[] data = new byte[32];
    Random random = new Random();
    random.nextBytes(data);

    Bytes bytes = new Bytes();

    Encoder<byte[]> encoder = new WithLengthBytesEncoder();
    encoder.encode(data, bytes);

    Bytes expected = new Bytes();
    expected.writeLong(data.length);
    expected.writeByteArray(data);

    assertArrayEquals(expected.getArray(), bytes.getArray());

    Decoder<Bytes> decoder = new WithLengthBytesDecoder();
    decoder = decoder.decode(Bytes.fromArray(bytes.getArray(), bytes.length()));
    assertTrue(decoder.isDone());

    Bytes actual = decoder.bind();
    assertArrayEquals(data, actual.getArray());
  }

}