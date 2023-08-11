package ai.swim.server.codec;

/**
 * An abstract, incremental, decoder that yields objects of type {@code T}.
 *
 * @param <T> the decoder's target type.
 */
public abstract class Decoder<T> {

  /**
   * Constructs a new decoder that is in a {@code done} state.
   *
   * @param decoder that decoded {@code T}. Calling {@code reset} on the done decoder will return this.
   * @param value   that the decoder produced.
   * @param <T>     the decoder's target type.
   * @return a decoder in the done state.
   */
  public static <T> Decoder<T> done(Decoder<T> decoder, T value) {
    return new DecoderDone<>(decoder, value);
  }

  /**
   * Returns whether the decoder is in a {@code done} state.
   */
  public boolean isDone() {
    throw new IllegalStateException("Decoder is not in a done state");
  }

  /**
   * Feed this decoder some data to decode.
   *
   * @param buffer to decode.
   * @return a decoder representing the output of the operation.
   * @throws DecoderException if it was not possible to decode the buffer.
   */
  public abstract Decoder<T> decode(Bytes buffer) throws DecoderException;

  /**
   * If this decoder is in a {@code done} state, take the decoded value.
   *
   * @return the decoded value.
   */
  public T bind() {
    throw new IllegalStateException("Decoder is not in a done state");
  }

  /**
   * Reset this decoder back to its original state.
   */
  public abstract Decoder<T> reset();

}
