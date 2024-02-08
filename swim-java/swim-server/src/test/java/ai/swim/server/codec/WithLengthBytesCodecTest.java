/*
 * Copyright 2015-2024 Swim Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.swim.server.codec;

import ai.swim.codec.data.ByteWriter;
import ai.swim.codec.data.ReadBuffer;
import ai.swim.codec.decoder.Decoder;
import ai.swim.codec.decoder.DecoderException;
import ai.swim.codec.encoder.Encoder;
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

    ByteWriter bytes = new ByteWriter();

    Encoder<byte[]> encoder = new WithLengthBytesEncoder();
    encoder.encode(data, bytes);

    ByteWriter expected = new ByteWriter();
    expected.writeLong(data.length);
    expected.writeByteArray(data);

    assertArrayEquals(expected.getArray(), bytes.getArray());

    Decoder<ReadBuffer> decoder = new WithLengthBytesDecoder();
    decoder = decoder.decode(bytes.reader());
    assertTrue(decoder.isDone());

    ReadBuffer actual = decoder.bind();
    assertArrayEquals(data, actual.getArray());
  }

}