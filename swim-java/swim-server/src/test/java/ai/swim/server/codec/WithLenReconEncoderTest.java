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

import ai.swim.codec.data.ByteReader;
import ai.swim.codec.data.ByteWriter;
import ai.swim.structure.Recon;
import ai.swim.structure.annotations.AutoForm;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class WithLenReconEncoderTest {

  @Test
  void testEncode() {
    String string = Recon.toStringCompact(new Prop(1, 2));
    int len = string.length();

    WithLenReconEncoder<Prop, PropWriter> encoder = new WithLenReconEncoder<>(new PropWriter());
    ByteWriter bytes = new ByteWriter();
    encoder.encode(new Prop(1, 2), bytes);

    ByteWriter expected = new ByteWriter();
    expected.writeLong(len);
    expected.writeByteArray(string.getBytes(StandardCharsets.UTF_8));

    ByteReader reader = bytes.reader();

    assertArrayEquals(expected.getArray(), reader.getArray());
  }

  @AutoForm
  public static class Prop {
    public int a;
    public int b;

    public Prop() {

    }

    public Prop(int a, int b) {
      this.a = a;
      this.b = b;
    }
  }

}