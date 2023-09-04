package ai.swim.server.codec;

import ai.swim.codec.data.ByteReader;
import ai.swim.codec.data.ByteWriter;
import ai.swim.codec.decoder.Decoder;
import ai.swim.codec.decoder.DecoderException;
import ai.swim.codec.encoder.Encoder;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WithLengthBytesCodecTest {

  @Test
  void roundTrip() throws DecoderException {
    byte[] data = new byte[32];
    Random random = new Random();
    random.nextBytes(data);

    ByteWriter bytes = new ByteWriter();

    Encoder<byte[]> encoder = new WithLengthBytesEncoder();
    encoder.encode(data, bytes);

    ByteWriter expected = new ByteWriter();
    expected.writeLong(data.length);
    expected.writeByteArray(data);

    assertArrayEquals(expected.getArray(), bytes.getArray());

    Decoder<ByteReader> decoder = new WithLengthBytesDecoder();
    decoder = decoder.decode(bytes.reader());
    assertTrue(decoder.isDone());

    ByteReader actual = decoder.bind();
    assertArrayEquals(data, actual.getArray());
  }

}