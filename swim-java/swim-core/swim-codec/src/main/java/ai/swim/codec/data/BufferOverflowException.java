package ai.swim.codec.data;

/**
 * Exception thrown when there is insufficient capacity in a buffer.
 */
public class BufferOverflowException extends RuntimeException {
  public BufferOverflowException() {
  }

  public BufferOverflowException(String message) {
    super(message);
  }
}
