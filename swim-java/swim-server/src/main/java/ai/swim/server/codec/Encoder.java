package ai.swim.server.codec;

/**
 * An interface for encoding an object.
 *
 * @param <T> that this encoder encodes.
 */
public interface Encoder<T> {

  /**
   * Encodes {@code target} into {@code buffer}.
   *
   * @param target to encode.
   * @param buffer to write into.
   */
  void encode(T target, Bytes buffer);

  /**
   * Encodes {@code target} into {@code buffer}, writing the length of {@code target} before the encoded bytes.
   *
   * @param target to encode.
   * @param buffer to write into.
   */
  default void encodeWithLen(T target, Bytes buffer) {
    int startIdx = buffer.remaining();
    buffer.writeInteger(0);

    int startLen = buffer.remaining();

    encode(target, buffer);
    buffer.writeInteger(buffer.remaining() - startLen, startIdx);
  }

}
