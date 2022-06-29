package ai.swim.structure.processor.recognizer;

import javax.lang.model.type.TypeMirror;

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
      BOOLEAN_RECOGNIZER = new PrimitiveRecognizerModel<>("ai.swim.structure.recognizer.std.ScalarRecognizer.BOOLEAN", false);
    }

    return BOOLEAN_RECOGNIZER;
  }

  public static RecognizerModel byteRecognizer() {
    if (BYTE_RECOGNIZER == null) {
      BYTE_RECOGNIZER = new PrimitiveRecognizerModel<>("ai.swim.structure.recognizer.std.ScalarRecognizer.BYTE", (byte) 0);
    }

    return BYTE_RECOGNIZER;
  }

  public static RecognizerModel shortRecognizer() {
    if (SHORT_RECOGNIZER == null) {
      SHORT_RECOGNIZER = new PrimitiveRecognizerModel<>("ai.swim.structure.recognizer.std.ScalarRecognizer.SHORT", (short) 0);
    }

    return SHORT_RECOGNIZER;
  }

  public static RecognizerModel intRecognizer() {
    if (INT_RECOGNIZER == null) {
      INT_RECOGNIZER = new PrimitiveRecognizerModel<>("ai.swim.structure.recognizer.std.ScalarRecognizer.INTEGER", 0);
    }

    return INT_RECOGNIZER;
  }

  public static RecognizerModel longRecognizer() {
    if (LONG_RECOGNIZER == null) {
      LONG_RECOGNIZER = new PrimitiveRecognizerModel<>("ai.swim.structure.recognizer.std.ScalarRecognizer.LONG", 0L);
    }

    return LONG_RECOGNIZER;
  }

  public static RecognizerModel charRecognizer() {
    if (CHAR_RECOGNIZER == null) {
      CHAR_RECOGNIZER = new PrimitiveRecognizerModel<>("ai.swim.structure.recognizer.std.ScalarRecognizer.CHAR", '\u0000');
    }

    return CHAR_RECOGNIZER;
  }

  public static RecognizerModel floatRecognizer() {
    if (FLOAT_RECOGNIZER == null) {
      FLOAT_RECOGNIZER = new PrimitiveRecognizerModel<>("ai.swim.structure.recognizer.std.ScalarRecognizer.FLOAT", 0f);
    }

    return FLOAT_RECOGNIZER;
  }

  public static RecognizerModel doubleRecognizer() {
    if (DOUBLE_RECOGNIZER == null) {
      DOUBLE_RECOGNIZER = new PrimitiveRecognizerModel<>("ai.swim.structure.recognizer.std.ScalarRecognizer.DOUBLE", 0d);
    }

    return DOUBLE_RECOGNIZER;
  }

  private final T defaultValue;
  private final String type;

  public PrimitiveRecognizerModel(String type, T defaultValue) {
    this.type = type;
    this.defaultValue = defaultValue;
  }

  @Override
  public String toString() {
    return "PrimitiveRecognizerModel{" + "defaultValue=" + defaultValue + ", type=" + type + '}';
  }

  @Override
  public String recognizerInitializer() {
    return this.type;
  }

  @Override
  public Object defaultValue() {
    return this.defaultValue;
  }

  @Override
  public TypeMirror type() {
    return null;
  }
}
