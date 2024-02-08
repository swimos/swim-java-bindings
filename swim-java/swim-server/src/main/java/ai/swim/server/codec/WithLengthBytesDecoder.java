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

import ai.swim.codec.Size;
import ai.swim.codec.data.ReadBuffer;
import ai.swim.codec.decoder.Decoder;
import ai.swim.codec.decoder.DecoderException;

/**
 * A decoder that decodes N bytes that are prefixed by their len.
 */
public class WithLengthBytesDecoder extends Decoder<ReadBuffer> {

  @Override
  public Decoder<ReadBuffer> decode(ReadBuffer buffer) throws DecoderException {
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
  public Decoder<ReadBuffer> reset() {
    return this;
  }

}
