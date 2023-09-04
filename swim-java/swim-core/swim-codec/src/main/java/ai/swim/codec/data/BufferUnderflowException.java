package ai.swim.codec.data;

/**
 * Exception thrown when get a relative operation reaches the source buffer's limit.
 */
public class BufferUnderflowException extends RuntimeException {
  public BufferUnderflowException() {
    super();
  }

  public BufferUnderflowException(String message) {
    super(message);
  }
}
