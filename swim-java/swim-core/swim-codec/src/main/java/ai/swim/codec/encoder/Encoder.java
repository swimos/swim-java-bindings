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
