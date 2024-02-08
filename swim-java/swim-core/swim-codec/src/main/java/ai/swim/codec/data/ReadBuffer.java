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

package ai.swim.codec.data;

import java.nio.ByteBuffer;

public interface ReadBuffer {
  static ByteBufferReader byteBuffer(ByteBuffer buffer) {
    return new ByteBufferReader(buffer);
  }

  static ByteReader fromArray(byte[] array) {
    return ByteReader.fromArray(array);
  }

  void advance(int by);

  int remaining();

  default boolean isEmpty() {
    return remaining() == 0;
  }

  int readPointer();

  byte getByte();

  byte peekByte();

  byte peekByte(int offset);

  int getInteger();

  int peekInteger();

  long getLong();

  long peekLong();

  byte[] getArray();

  byte[] peekArray();

  int getByteArray(byte[] into);

  ReadBuffer splitTo(int at);

  ReadBuffer splitOff(int at);

  void unsplit(ReadBuffer from);

  ReadBuffer clone();

}
