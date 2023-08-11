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
   * @param buffer to write to.
   */
  void encode(T target, Bytes buffer);

}
