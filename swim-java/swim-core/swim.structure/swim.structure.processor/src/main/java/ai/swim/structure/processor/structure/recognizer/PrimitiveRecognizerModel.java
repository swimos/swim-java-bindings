package ai.swim.structure.processor.structure.recognizer;

public class PrimitiveRecognizerModel<T> extends RecognizerModel {

  private static RecognizerModel BYTE_RECOGNIZER;
  private static RecognizerModel BOOLEAN_RECOGNIZER;
  private static RecognizerModel SHORT_RECOGNIZER;
  private static RecognizerModel INT_RECOGNIZER;
  private static RecognizerModel LONG_RECOGNIZER;
  private static RecognizerModel CHAR_RECOGNIZER;
  private static RecognizerModel FLOAT_RECOGNIZER;
  private static RecognizerModel DOUBLE_RECOGNIZER;

  public static RecognizerModel booleanRecognizer() {
    if (BOOLEAN_RECOGNIZER == null) {
      BOOLEAN_RECOGNIZER = new PrimitiveRecognizerModel<>(Boolean.TYPE, false);
    }

    return BOOLEAN_RECOGNIZER;
  }

  public static RecognizerModel byteRecognizer() {
    if (BYTE_RECOGNIZER == null) {
      BYTE_RECOGNIZER = new PrimitiveRecognizerModel<>(Byte.TYPE, (byte) 0);
    }

    return BYTE_RECOGNIZER;
  }

  public static RecognizerModel shortRecognizer() {
    if (SHORT_RECOGNIZER == null) {
      SHORT_RECOGNIZER = new PrimitiveRecognizerModel<>(Short.TYPE, (short) 0);
    }

    return SHORT_RECOGNIZER;
  }

  public static RecognizerModel intRecognizer() {
    if (INT_RECOGNIZER == null) {
      INT_RECOGNIZER = new PrimitiveRecognizerModel<>(Integer.TYPE, 0);
    }

    return INT_RECOGNIZER;
  }

  public static RecognizerModel longRecognizer() {
    if (LONG_RECOGNIZER == null) {
      LONG_RECOGNIZER = new PrimitiveRecognizerModel<>(Long.TYPE, 0L);
    }

    return LONG_RECOGNIZER;
  }

  public static RecognizerModel charRecognizer() {
    if (CHAR_RECOGNIZER == null) {
      CHAR_RECOGNIZER = new PrimitiveRecognizerModel<>(Character.TYPE, '\u0000');
    }

    return CHAR_RECOGNIZER;
  }

  public static RecognizerModel floatRecognizer() {
    if (FLOAT_RECOGNIZER == null) {
      FLOAT_RECOGNIZER = new PrimitiveRecognizerModel<>(Float.TYPE, 0f);
    }

    return FLOAT_RECOGNIZER;
  }

  public static RecognizerModel doubleRecognizer() {
    if (DOUBLE_RECOGNIZER == null) {
      DOUBLE_RECOGNIZER = new PrimitiveRecognizerModel<>(Double.TYPE, 0d);
    }

    return DOUBLE_RECOGNIZER;
  }

  private final T defaultValue;
  private final Class<T> type;

  public PrimitiveRecognizerModel(Class<T> type, T defaultValue) {
    this.type = type;
    this.defaultValue = defaultValue;
  }

  public T getDefaultValue() {
    return defaultValue;
  }

  public Class<T> getType() {
    return type;
  }

  @Override
  public String toString() {
    return "PrimitiveRecognizerModel{" +
        "defaultValue=" + defaultValue +
        ", type=" + type +
        '}';
  }
}
