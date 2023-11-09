package ai.swim.server.codec;

/**
 * An exception thrown when it was not possible to decode the buffer.
 */
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
