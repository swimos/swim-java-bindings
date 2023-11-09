package ai.swim.server.codec;

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
  void encode(T target, Bytes buffer);

  /**
   * Encodes {@code target} into {@code buffer}, writing the length of {@code target} before the encoded bytes as a long.
   *
   * @param target to encode.
   * @param buffer to write into.
   */
  default void encodeWithLongLen(T target, Bytes buffer) {
    int startIdx = buffer.remaining();
    buffer.writeLong(0);

    int startLen = buffer.remaining();

    encode(target, buffer);
    buffer.writeLong(buffer.remaining() - startLen, startIdx);
  }

  /**
   * Encodes {@code target} into {@code buffer}, writing the length of {@code target} before the encoded bytes as an int.
   *
   * @param target to encode.
   * @param buffer to write into.
   */
  default void encodeWithIntLen(T target, Bytes buffer) {
    int startIdx = buffer.remaining();
    buffer.writeInteger(0);

    int startLen = buffer.remaining();

    encode(target, buffer);
    buffer.writeInteger(buffer.remaining() - startLen, startIdx);
  }

}
