package ai.swim.server.codec;

import java.io.OutputStream;
import java.util.Arrays;

/**
 * A byte buffer providing read and write access to an underlying array. This buffer encodes and decodes in big endian
 * format and provides methods for inserting and retrieving various data types; maintaining independent read and write
 * pointers.
 * <p>
 * This class is not thread safe.
 */
public class Bytes {
  /// The underlying byte array.
  private byte[] buffer;
  /// Next element to read from.
  private int readPointer;
  /// Next element to write to.
  private int writePointer;

  public Bytes(int initialCapacity) {
    if (initialCapacity < 0) {
      throw new IllegalArgumentException("Initial capacity < 0 "
                                             + initialCapacity);
    }
    buffer = new byte[initialCapacity];
  }

  public Bytes() {
    this(64);
  }

  public static Bytes fromArray(byte[] array, int len) {
    Bytes bytes = new Bytes(len);
    bytes.writeByteArray(array, len);
    return bytes;
  }

  public static Bytes fromArray(byte[] array) {
    return fromArray(array, array.length);
  }

  public OutputStream outputStream() {
    return new BytesOutputStream(this);
  }

  /**
   * Reserves an extra {@code extra} elements. This is a no-op if the buffer already has sufficient capacity.
   *
   * @throws IllegalArgumentException is extra < 0.
   */
  public void reserve(int extra) {
    if (extra < 0) {
      throw new IllegalArgumentException("extra < 0");
    }

    if (extra > buffer.length - writePointer) {
      checkedAdd(buffer.length, extra);
      buffer = Arrays.copyOf(buffer, buffer.length + extra);
    }
  }

  private int checkedAdd(int a, int b) {
    int r = a + b;
    if (((a ^ r) & (b ^ r)) < 0) {
      throw new BufferOverflowException();
    } else {
      return r;
    }
  }

  /**
   * Advances the read pointer by {@code by} elements.
   */
  public void advance(int by) {
    if (by > buffer.length) {
      throw new IllegalArgumentException(String.format("%d > %d", by, buffer.length));
    } else {
      readPointer = checkedAdd(readPointer, by);
    }
  }

  /**
   * Returns the difference between the write and read pointers.
   */
  public int remaining() {
    return writePointer - readPointer;
  }

  /**
   * Returns the total number of bytes written.
   */
  public int length() {
    return writePointer;
  }

  /**
   * Returns the total capacity.
   */
  public int capacity() {
    return buffer.length;
  }

  private void write(byte b) {
    reserve(writePointer + 1);
    buffer[writePointer] = b;
    writePointer += 1;
  }

  private void writeAt(byte b, int at) {
    if (at > buffer.length) {
      throw new IllegalArgumentException("at > buffer length");
    } else {
      buffer[at] = b;
    }
  }

  /**
   * Writes an integer in big endian format.
   */
  public void writeInteger(int b) {
    reserve(4);
    writeByte(((byte) ((b >> 24) & 0xff)));
    writeByte((byte) ((b >> 16) & 0xff));
    writeByte((byte) ((b >> 8) & 0xff));
    writeByte((byte) (b & 0xff));
  }

  /**
   * Writes an integer in big endian format starting from the provided offset.
   */
  public void writeInteger(int b, int startAt) {
    writeByte(((byte) ((b >> 24) & 0xff)), startAt);
    writeByte((byte) ((b >> 16) & 0xff), startAt + 1);
    writeByte((byte) ((b >> 8) & 0xff), startAt + 2);
    writeByte((byte) (b & 0xff), startAt + 3);
  }

  /**
   * Gets a byte and advances the cursor by 1.
   */
  public byte getByte() {
    return buffer[readPointer++];
  }

  /**
   * Peeks a byte without advancing the read pointer.
   */
  public byte peekByte() {
    return buffer[readPointer];
  }

  /**
   * Peeks a byte at the provided offset without advancing the read pointer.
   */
  public byte peekByte(int offset) {
    return buffer[readPointer + offset];
  }

  /**
   * Gets an integer in big endian format.
   */
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
   * Writes a long in big endian format.
   */
  public void writeLong(long l) {
    reserve(8);
    writeByte((byte) ((l >> 56) & 0xff));
    writeByte((byte) ((l >> 48) & 0xff));
    writeByte((byte) ((l >> 40) & 0xff));
    writeByte((byte) ((l >> 32) & 0xff));
    writeByte((byte) ((l >> 24) & 0xff));
    writeByte((byte) ((l >> 16) & 0xff));
    writeByte((byte) ((l >> 8) & 0xff));
    writeByte((byte) (l & 0xff));
  }

  /**
   * Writes a byte.
   */
  public void writeByte(byte b) {
    reserve(1);
    write(b);
  }

  public void writeByte(byte b, int at) {
    writeAt(b, at);
  }

  /**
   * Returns the underlying array.
   */
  public byte[] getArray() {
    return buffer;
  }

  @Override
  public String toString() {
    return "Bytes{" +
        "buffer=" + Arrays.toString(buffer) +
        ", writePointer=" + writePointer +
        ", readPointer=" + readPointer +
        '}';
  }

  /**
   * Writes a byte array.
   */
  public void writeByteArray(byte[] target) {
    writeByteArray(target, target.length);
  }

  /**
   * Writes {@code count} elements from {@code target}.
   */
  public void writeByteArray(byte[] target, int count) {
    reserve(count);
    System.arraycopy(target, 0, buffer, writePointer, count);
    writePointer += count;
  }

  /**
   * Fills {@code bytes} with as many available bytes as possible. This is the minimum between the number of elements
   * remaining and the length of {@code bytes}.
   *
   * @param bytes to populate
   * @return the number of bytes written.
   */
  public int getByteArray(byte[] bytes) {
    int readCount = Math.min(buffer.length - readPointer, bytes.length);
    checkedAdd(buffer.length, readCount);
    System.arraycopy(buffer, readPointer, bytes, 0, readCount);
    readPointer += readCount;
    return readCount;
  }

  public Bytes splitTo(int at) {
    if (at <= readPointer) {
      throw new IllegalArgumentException(String.format("Out of bounds: %d <= %s", at, readPointer));
    } else {
      byte[] other = new byte[at];
      System.arraycopy(buffer, readPointer, other, 0, at);
      readPointer = at;
      return Bytes.fromArray(other, at);
    }
  }

}
