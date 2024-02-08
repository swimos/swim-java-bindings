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
