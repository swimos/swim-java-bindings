package ai.swim.server.codec;

public class DecoderException extends Exception {
  public DecoderException() {
  }

  public DecoderException(String message) {
    super(message);
  }

  public DecoderException(String message, Throwable cause) {
    super(message, cause);
  }
}
