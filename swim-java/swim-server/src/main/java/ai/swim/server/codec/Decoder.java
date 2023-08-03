package ai.swim.server.codec;

public abstract class Decoder<T> {

  public static <T> Decoder<T> done(Decoder<T> decoder, T value) {
    return new DecoderDone<>(decoder, value);
  }

  public boolean isDone() {
    throw new IllegalStateException("Decoder is not in a done state");
  }

  public abstract Decoder<T> decode(Bytes buffer) throws DecoderException;

  public T bind() {
    throw new IllegalStateException("Decoder is not in a done state");
  }

  public abstract Decoder<T> reset();

}
