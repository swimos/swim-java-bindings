package ai.swim.structure.form.recognizer;

import java.util.concurrent.ConcurrentHashMap;
import ai.swim.structure.form.recognizer.primitive.IntegerRecognizer;
import ai.swim.structure.form.recognizer.primitive.StringRecognizer;

public class RecognizerProxy {

  private static final ConcurrentHashMap<Class<?>, Recognizer<?>> lut;

  static {
    ConcurrentHashMap<Class<?>, Recognizer<?>> recognizers = new ConcurrentHashMap<>();
    recognizers.put(Integer.class, new IntegerRecognizer());
    recognizers.put(String.class, new StringRecognizer());

    // todo build at compile time

    lut = recognizers;
  }

  @SuppressWarnings("unchecked")
  public static <T> Recognizer<T> lookup(Class<T> clazz) {
    if (clazz == null) {
      throw new NullPointerException();
    }

    Recognizer<?> recognizer = RecognizerProxy.lut.get(clazz);
    if (recognizer == null) {
      throw new RuntimeException("Failed to find recognizer for: " + clazz.getCanonicalName());
    } else {
      return (Recognizer<T>) recognizer;
    }
  }

}
