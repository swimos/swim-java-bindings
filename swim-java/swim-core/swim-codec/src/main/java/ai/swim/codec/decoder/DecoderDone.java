package ai.swim.codec.decoder;


import ai.swim.codec.data.ByteReader;

/**
 * A decoder in a done state.
 *
 * @param <T> decoder's target type.
 */
public class DecoderDone<T> extends Decoder<T> {
  private final Decoder<T> decoder;
  private final T value;

  public DecoderDone(Decoder<T> decoder, T value) {
    this.decoder = decoder;
    this.value = value;
  }

  @Override
  public Decoder<T> decode(ByteReader buffer) {
    throw new IllegalStateException("Decoder complete");
  }

  @Override
  public boolean isDone() {
    return true;
  }

  @Override
  public T bind() {
    return value;
  }

  @Override
  public Decoder<T> reset() {
    return decoder.reset();
  }

}
