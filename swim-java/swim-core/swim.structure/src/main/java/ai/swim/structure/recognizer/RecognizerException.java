package ai.swim.structure.recognizer;

public class RecognizerException extends RuntimeException {

  public RecognizerException() {
    super();
  }

  public RecognizerException(String message) {
    super(message);
  }

  public RecognizerException(String message, Throwable cause) {
    super(message, cause);
  }

  public RecognizerException(Throwable cause) {
    super(cause);
  }
}
