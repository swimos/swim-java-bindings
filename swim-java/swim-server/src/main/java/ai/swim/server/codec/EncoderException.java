package ai.swim.server.codec;

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
