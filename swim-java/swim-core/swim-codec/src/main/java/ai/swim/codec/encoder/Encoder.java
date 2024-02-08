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

package ai.swim.codec.encoder;

import ai.swim.codec.data.ByteWriter;

/**
 * An interface for encoding an object.
 *
 * @param <T> that this encoder encodes.
 */
@FunctionalInterface
public interface Encoder<T> {

  /**
   * Encodes {@code target} into {@code buffer}.
   *
   * @param target to encode.
   * @param buffer to write into.
   */
  void encode(T target, ByteWriter buffer);

  /**
   * Encodes {@code target} into {@code buffer}, writing the length of {@code target} before the encoded bytes as an int.
   *
   * @param target to encode.
   * @param buffer to write into.
   */
  default void encodeWithLen(T target, ByteWriter buffer) {
    int startIdx = buffer.writePosition();
    buffer.writeInteger(0);

    int startLen = buffer.writePosition();

    encode(target, buffer);
    buffer.writeInteger(buffer.writePosition() - startLen, startIdx);
  }

}
