package ai.swim.codec.data;

import ai.swim.codec.Size;
import java.io.OutputStream;
import java.util.Arrays;
import static ai.swim.codec.data.ByteUtils.accumulateHashCode;
import static ai.swim.codec.data.ByteUtils.accumulateHashcode;
import static ai.swim.codec.data.ByteUtils.boundedHashcode;
import static ai.swim.codec.data.ByteUtils.checkedAdd;

public class ByteWriter {
  /// The underlying byte array.
  private byte[] buffer;
  /// Next element to write to.
  private int writePointer;
  /// Rolling hashcode calculation for comparing arrays more efficiently.
  private int elementHashcode;

  public ByteWriter(int initialCapacity) {
    if (initialCapacity < 0) {
      throw new IllegalArgumentException("Initial capacity < 0 " + initialCapacity);
    }
    buffer = new byte[initialCapacity];
    elementHashcode = 1;
  }

  public ByteWriter() {
    this(64);
  }

  private ByteWriter(byte[] buffer, int writePointer, int elementHashcode) {
    this.buffer = buffer;
    this.writePointer = writePointer;
    this.elementHashcode = elementHashcode;
  }

  public static ByteWriter fromArray(byte[] array, int len) {
    return new ByteWriter(array, len, boundedHashcode(array, len));
  }

  public static ByteWriter fromArray(byte[] array) {
    return fromArray(array, array.length);
  }

  public OutputStream outputStream() {
    return new ByteWriterOutputStream(this);
  }

  /**
   * Ensures that the buffer has the capacity to hold at least 'extra' elements. This is a no-op if the buffer already
   * has sufficient capacity. If there is insufficient capacity and the buffer will not overflow {@link Integer#MAX_VALUE},
   * then the buffer will grow a capacity that is the next power of two.
   *
   * @throws IllegalArgumentException is extra < 0.
   * @throws BufferOverflowException  if the buffer would overflow {@link Integer#MAX_VALUE}.
   */
  public void reserve(int extra) {
    if (extra < 0) {
      throw new IllegalArgumentException("extra < 0");
    }

    // If the current capacity minus the number of elements filled > the required extra capacity then grow to the
    // next power of 2.
    int required = checkedAdd(remaining(), extra);
    if (required > buffer.length) {
      int pow2Length = Integer.highestOneBit(required);
      if (pow2Length != required) {
        pow2Length = pow2Length << 1;
      }
      buffer = Arrays.copyOf(buffer, pow2Length);
    }
  }

  /**
   * Returns the remaining number of bytes that can be written.
   * <p>
   * Note: this is not guaranteed to be consistent across write calls due to resizing operations. If the current index
   * is required then use {@link ByteWriter#writePosition()}
   */
  public int remaining() {
    return buffer.length - writePointer;
  }

  /**
   * Returns the current write position.
   */
  public int writePosition() {
    return writePointer;
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
    reserve(Size.BYTE);
    buffer[writePointer++] = b;
    elementHashcode = accumulateHashcode(elementHashcode, b);
  }

  private void writeAt(byte b, int at) {
    if (at > buffer.length) {
      throw new IllegalArgumentException("at > buffer length");
    } else if (at < 0) {
      throw new IllegalArgumentException("At < 0");
    } else {
      buffer[at] = b;
      elementHashcode = accumulateHashcode(elementHashcode, b);
    }
  }

  /**
   * Writes an integer in big endian format.
   */
  public void writeInteger(int b) {
    reserve(Size.INT);
    writeByte(((byte) ((b >> 24) & 0xff)));
    writeByte((byte) ((b >> 16) & 0xff));
    writeByte((byte) ((b >> 8) & 0xff));
    writeByte((byte) (b & 0xff));
  }

  /**
   * Writes an integer in big endian format starting from the provided offset.
   */
  public void writeInteger(int b, int startAt) {
    reserve(Size.INT);
    writeByte(((byte) ((b >> 24) & 0xff)), startAt);
    writeByte((byte) ((b >> 16) & 0xff), startAt + 1);
    writeByte((byte) ((b >> 8) & 0xff), startAt + 2);
    writeByte((byte) (b & 0xff), startAt + 3);
  }

  /**
   * Writes a long in big endian format.
   */
  public void writeLong(long l) {
    reserve(Size.LONG);
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
   * Writes a long in big endian format starting from the provided offset.
   */
  public void writeLong(long l, int startAt) {
    reserve(Size.LONG);
    writeByte((byte) ((l >> 56) & 0xff), startAt);
    writeByte((byte) ((l >> 48) & 0xff), startAt + 1);
    writeByte((byte) ((l >> 40) & 0xff), startAt + 2);
    writeByte((byte) ((l >> 32) & 0xff), startAt + 3);
    writeByte((byte) ((l >> 24) & 0xff), startAt + 4);
    writeByte((byte) ((l >> 16) & 0xff), startAt + 5);
    writeByte((byte) ((l >> 8) & 0xff), startAt + 6);
    writeByte((byte) (l & 0xff), startAt + 7);
  }

  /**
   * Writes a byte.
   */
  public void writeByte(byte b) {
    write(b);
  }

  /**
   * Writes a byte at the provided offset
   */
  public void writeByte(byte b, int at) {
    writeAt(b, at);
  }

  /**
   * Returns the underlying array.
   */
  public byte[] getArray() {
    return Arrays.copyOf(buffer, writePointer);
  }

  @Override
  public String toString() {
    return "Bytes{" + "buffer=" + Arrays.toString(buffer) + ", writePointer=" + writePointer + '}';
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
    elementHashcode = accumulateHashCode(elementHashcode, target, count);
  }

  /**
   * Returns a {@link ByteReader} backed by the data that has been written into this {@link ByteWriter}.
   */
  public ByteReader reader() {
    return ByteReader.fromByteWriter(this);
  }

  @Override
  protected ByteWriter clone() {
    return new ByteWriter(Arrays.copyOf(buffer, buffer.length), writePointer, elementHashcode);
  }

  byte[] getBuffer() {
    return buffer;
  }

  int getWritePointer() {
    return writePointer;
  }

  int getElementHashcode() {
    return elementHashcode;
  }

}
