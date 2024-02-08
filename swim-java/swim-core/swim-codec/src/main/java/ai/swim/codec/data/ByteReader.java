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

import java.util.Arrays;
import static ai.swim.codec.data.ByteUtils.boundedHashcode;
import static ai.swim.codec.data.ByteUtils.boundedToString;
import static ai.swim.codec.data.ByteUtils.checkedAdd;

public class ByteReader implements ReadBuffer {
  private static final int NO_HASHCODE = -1;
  /// Hashcode of the data within 'buffer'.
  private final int elemHashcode;
  /// The underlying byte array.
  private byte[] buffer;
  /// Next element to read from.
  private int readPointer;
  /// The number of elements contains within 'buffer'.
  private int len;

  private ByteReader(byte[] buffer, int readPointer, int len, int elemHashcode) {
    this.buffer = buffer;
    this.readPointer = readPointer;
    this.len = len;
    this.elemHashcode = elemHashcode;
  }

  public static ByteReader fromArray(byte[] buffer) {
    return new ByteReader(buffer, 0, buffer.length, NO_HASHCODE);
  }

  public static ByteReader fromArray(byte[] buffer, int len) {
    return new ByteReader(buffer, 0, len, boundedHashcode(buffer, len));
  }

  public static ByteReader empty() {
    return new ByteReader(new byte[] {}, 0, 0, 1);
  }

  public static ByteReader fromByteWriter(ByteWriter writer) {
    return new ByteReader(writer.getBuffer(), 0, writer.getWritePointer(), writer.getElementHashcode());
  }

  /**
   * Advances the read pointer by {@code by} elements.
   */
  @Override
  public void advance(int by) {
    int remaining = remaining();
    if (by > remaining) {
      throw new BufferOverflowException(String.format("Count > remaining %d > %d", by, remaining));
    } else {
      readPointer = checkedAdd(readPointer, by);
    }
  }

  /**
   * Returns the remaining number of bytes to be read.
   */
  @Override
  public int remaining() {
    return len - readPointer;
  }

  /**
   * Returns the current index to be read.
   */
  @Override
  public int readPointer() {
    return readPointer;
  }

  /**
   * Gets a byte and advances the read pointer by 1.
   */
  @Override
  public byte getByte() {
    if (readPointer > len) {
      throw new BufferUnderflowException();
    }

    return buffer[readPointer++];
  }

  /**
   * Peeks a byte without advancing the read pointer.
   */
  @Override
  public byte peekByte() {
    if (readPointer > len) {
      throw new BufferUnderflowException();
    }

    return buffer[readPointer];
  }

  /**
   * Peeks a byte at the provided offset without advancing the read pointer.
   */
  @Override
  public byte peekByte(int offset) {
    if (readPointer + offset >= len) {
      throw new IndexOutOfBoundsException();
    }

    return buffer[readPointer + offset];
  }

  /**
   * Gets an integer in big endian format.
   */
  @Override
  public int getInteger() {
    int a = (getByte() & 0xff) << 24;
    int b = (getByte() & 0xff) << 16;
    int c = (getByte() & 0xff) << 8;
    int d = getByte() & 0xff;
    return a | b | c | d;
  }

  /**
   * Peeks an integer without advancing the read pointer.
   */
  @Override
  public int peekInteger() {
    int a = (peekByte() & 0xff) << 24;
    int b = (peekByte(1) & 0xff) << 16;
    int c = (peekByte(2) & 0xff) << 8;
    int d = peekByte(3) & 0xff;
    return a | b | c | d;
  }

  /**
   * Gets a long in big endian format.
   */
  @Override
  public long getLong() {
    long a = (long) (getByte() & 0xff) << 56;
    long b = (long) (getByte() & 0xff) << 48;
    long c = (long) (getByte() & 0xff) << 40;
    long d = (long) (getByte() & 0xff) << 32;
    long e = (long) (getByte() & 0xff) << 24;
    long f = (long) (getByte() & 0xff) << 16;
    long g = (long) (getByte() & 0xff) << 8;
    long h = (long) getByte() & 0xff;
    return a | b | c | d | e | f | g | h;
  }

  /**
   * Peeks a long without advancing the read pointer.
   */
  @Override
  public long peekLong() {
    long a = (long) (peekByte() & 0xff) << 56;
    long b = (long) (peekByte(1) & 0xff) << 48;
    long c = (long) (peekByte(2) & 0xff) << 40;
    long d = (long) (peekByte(3) & 0xff) << 32;
    long e = (long) (peekByte(4) & 0xff) << 24;
    long f = (long) (peekByte(5) & 0xff) << 16;
    long g = (long) (peekByte(6) & 0xff) << 8;
    long h = (long) peekByte(7) & 0xff;
    return a | b | c | d | e | f | g | h;
  }

  /**
   * Allocates a new array containing the remaining data to be read and advances the read pointer by the remaining
   * number of elements to be read.
   */
  @Override
  public byte[] getArray() {
    int remaining = remaining();
    byte[] dst = new byte[remaining];
    System.arraycopy(buffer, readPointer, dst, 0, remaining);
    readPointer += remaining;
    return dst;
  }

  /**
   * Allocates a new array containing the remaining data to be read.
   */
  @Override
  public byte[] peekArray() {
    byte[] dst = new byte[remaining()];
    System.arraycopy(buffer, readPointer, dst, 0, remaining());
    return dst;
  }

  /**
   * Fills {@code bytes} with as many available bytes as possible. This is the minimum between the number of elements
   * remaining and the length of {@code bytes}.
   *
   * @param bytes to populate
   * @return the number of bytes written.
   */
  @Override
  public int getByteArray(byte[] bytes) {
    int readCount = Math.min(remaining(), bytes.length);
    checkedAdd(len, readCount);
    System.arraycopy(buffer, readPointer, bytes, 0, readCount);
    readPointer += readCount;
    return readCount;
  }

  @Override
  public ByteReader splitTo(int at) {
    int end = readPointer + at;
    if (end > len) {
      throw new BufferOverflowException(String.format("At >= len: %s >= %s", at, len));
    }

    ByteReader rem = new ByteReader(buffer, readPointer, end, elemHashcode);
    readPointer = readPointer == 0 ? at : readPointer + at;
    return rem;
  }

  @Override
  public ByteReader splitOff(int at) {
    int end = readPointer + at;
    if (end > len) {
      throw new BufferOverflowException(String.format("At >= len: %s >= %s", at, len));
    }

    ByteReader rem = new ByteReader(buffer, at, len, elemHashcode);
    rem.readPointer = at;

    return rem;
  }

  /**
   * Extends this {@link ByteReader} with the content's of {@code from} and voids {@code from}.
   * <p>
   * If this {@link ByteReader} contains the same data as {@code from} then this operation simply increments this
   * {@link ByteReader}'s length and voids {@code from}. Otherwise, this {@link ByteReader}'s backing buffer is extended
   * with the data contained in {@code from}.
   * <p>
   * If this {@link ByteReader} is empty, then this {@link ByteReader}'s pointers and buffer are set to {@code from}'s.
   *
   * @param other to extend this {@link ByteReader}'s buffer with.
   */
  @Override
  public void unsplit(ReadBuffer other) {
    if (!(other instanceof ByteReader)) {
      throw new ClassCastException();
    } else {
      ByteReader from = (ByteReader) other;
      if (!from.isEmpty()) {
        if (isEmpty()) {
          buffer = from.buffer;
          readPointer = from.readPointer;
          len = from.len;
        } else {
          int remaining = remaining();
          int newLen = from.remaining() + remaining;
          boolean equal;

          if (elemHashcode == NO_HASHCODE || from.elemHashcode == NO_HASHCODE) {
            equal = Arrays.equals(buffer, from.buffer);
          } else {
            equal = elemHashcode == from.elemHashcode;
          }

          // If both 'this' and 'from' contain to the same data then we can skip reallocating an array.
          if (!equal) {
            byte[] newBuf = new byte[newLen];

            System.arraycopy(buffer, readPointer, newBuf, 0, remaining);
            System.arraycopy(from.buffer, from.readPointer, newBuf, remaining, from.remaining());

            buffer = newBuf;
          }

          len = newLen;
        }

        // Void 'from'.
        from.readPointer = from.len;
      }
    }
  }

  @Override
  public ByteReader clone() {
    return new ByteReader(buffer, readPointer, len, elemHashcode);
  }

  @Override
  public String toString() {
    return "ByteReader{" + "buffer=" + boundedToString(
        buffer,
        len) + ", readPointer=" + readPointer + ", len=" + len + ", elemHashcode=" + elemHashcode + '}';
  }

}
