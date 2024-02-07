package ai.swim.codec.encoder;

/**
 * An exception that is thrown when an error is encountered by an {@link Encoder}.
 */
public class EncoderException extends RuntimeException {
  public EncoderException(String message) {
    super(message);
  }

  public EncoderException(String message, Throwable cause) {
    super(message, cause);
  }

  public EncoderException(Throwable cause) {
    super(cause);
  }
}
