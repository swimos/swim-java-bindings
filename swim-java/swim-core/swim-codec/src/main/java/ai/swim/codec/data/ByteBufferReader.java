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

public class ByteBufferReader implements ReadBuffer {
  private final ByteBuffer buffer;

  ByteBufferReader(ByteBuffer buffer) {
    this.buffer = buffer;
  }

  @Override
  public void advance(int by) {
    if (by > buffer.remaining()) {
      throw new BufferOverflowException();
    } else {
      buffer.position(readPointer() + by);
    }
  }

  @Override
  public int remaining() {
    return buffer.remaining();
  }

  @Override
  public int readPointer() {
    return buffer.position();
  }

  @Override
  public byte getByte() {
    return buffer.get();
  }

  @Override
  public byte peekByte() {
    buffer.mark();
    byte b = buffer.get();
    buffer.reset();
    return b;
  }

  @Override
  public byte peekByte(int offset) {
    int idx = readPointer() + offset;
    if (idx >= buffer.capacity()) {
      throw new BufferOverflowException();
    }

    return buffer.get(idx);
  }

  @Override
  public int getInteger() {
    return buffer.getInt();
  }

  @Override
  public int peekInteger() {
    buffer.mark();
    int i = buffer.getInt();
    buffer.reset();
    return i;
  }

  @Override
  public long getLong() {
    return buffer.getLong();
  }

  @Override
  public long peekLong() {
    buffer.mark();
    long l = buffer.getLong();
    buffer.reset();
    return l;
  }

  @Override
  public byte[] getArray() {
    byte[] buf = new byte[buffer.remaining()];
    buffer.get(buf);
    return buf;
  }

  @Override
  public byte[] peekArray() {
    buffer.mark();
    byte[] buf = new byte[buffer.remaining()];
    buffer.get(buf);
    buffer.reset();
    return buf;
  }

  @Override
  public int getByteArray(byte[] into) {
    if (into == null) {
      throw new NullPointerException();
    }
    int toWrite = Math.min(into.length, remaining());
    buffer.get(into, 0, toWrite);
    return toWrite;
  }

  @Override
  public ReadBuffer splitTo(int at) {
    int limit = buffer.limit();
    int readPointer = readPointer();
    int end = readPointer + at;

    if (end > limit) {
      throw new BufferOverflowException(String.format("At >= len: %s >= %s", at, limit));
    }

    ByteBuffer rem = buffer
        .duplicate()
        .position(readPointer)
        .limit(end);
    buffer.position(readPointer == 0 ? at : readPointer + at);

    return new ByteBufferReader(rem);
  }

  @Override
  public ReadBuffer splitOff(int at) {
    throw new AssertionError();
  }

  @Override
  public void unsplit(ReadBuffer from) {
    throw new AssertionError();
  }

  @Override
  public ReadBuffer clone() {
    return new ByteBufferReader(buffer.duplicate());
  }

}
