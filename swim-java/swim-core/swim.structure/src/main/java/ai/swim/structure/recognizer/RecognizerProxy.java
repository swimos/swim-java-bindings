package ai.swim.structure.recognizer;


import ai.swim.structure.annotations.AutoloadedRecognizer;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class RecognizerProxy {

  private static final RecognizerProxy INSTANCE = new RecognizerProxy();
  private final ConcurrentHashMap<Class<?>, Supplier<Recognizer<?>>> recognizers;

  private RecognizerProxy() {
    recognizers = loadRecognizers();
  }

  private static ConcurrentHashMap<Class<?>, Supplier<Recognizer<?>>> loadRecognizers() {
    ConcurrentHashMap<Class<?>, Supplier<Recognizer<?>>> recognizers = new ConcurrentHashMap<>();
    recognizers.put(Integer.class, () -> ScalarRecognizer.BOXED_INTEGER);
    recognizers.put(String.class, () -> ScalarRecognizer.STRING);
    recognizers.put(Object.class, ObjectRecognizer::new);

    loadFromClassPath(recognizers);

    return recognizers;
  }

  private static void loadFromClassPath(ConcurrentHashMap<Class<?>, Supplier<Recognizer<?>>> recognizers) {
    Reflections reflections = new Reflections(ClasspathHelper.forJavaClassPath(), "ai.swim");
    Set<Class<?>> classes = reflections.getTypesAnnotatedWith(AutoloadedRecognizer.class);

    for (Class<?> clazz : classes) {
      AutoloadedRecognizer annotation = clazz.getAnnotation(AutoloadedRecognizer.class);
      Class<?> targetClass = annotation.value();

      if (!Recognizer.class.isAssignableFrom(clazz)) {
        String error = String.format("%s is annotated with @%s(%s.class) but %s does not extend %s",
            clazz.getCanonicalName(),
            AutoloadedRecognizer.class.getSimpleName(),
            targetClass.getCanonicalName(),
            clazz.getCanonicalName(),
            Recognizer.class.getSimpleName()
        );

        throw new RuntimeException(error);
      }

      if (!clazz.getNestHost().equals(clazz)) {
        if (!Modifier.isStatic(clazz.getModifiers())) {
          throw new RuntimeException("Nested non-static classes are not supported by recognizers: " + clazz.getCanonicalName());
        }
      }

      try {
        Constructor<?> constructor = clazz.getConstructor();
        recognizers.put(targetClass, () -> {
          try {
            return (Recognizer<?>) constructor.newInstance();
          } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(String.format("Failed to created a new instance of recognizer '%s'", clazz.getCanonicalName()), e);
          }
        });
      } catch (NoSuchMethodException e) {
        throw new RuntimeException(String.format("Recognizer '%s' does not contain a zero-arg constructor", clazz.getCanonicalName()), e);
      }
    }
  }

  public static RecognizerProxy getInstance() {
    return RecognizerProxy.INSTANCE;
  }

  @SuppressWarnings("unchecked")
  public <T> Recognizer<T> lookup(Class<T> clazz) {
    if (clazz == null) {
      throw new NullPointerException();
    }

    Supplier<Recognizer<?>> recognizer = this.recognizers.get(clazz);

    if (recognizer == null) {
      throw new RuntimeException("Failed to find recognizer for: " + clazz.getCanonicalName());
    } else {
      return (Recognizer<T>) recognizer.get();
    }
  }

  public <C, R extends Recognizer<C>> void registerRecognizer(Class<C> clazz, Supplier<R> recognizer) {
    this.registerRecognizerInternal(clazz, recognizer);
  }

  @SuppressWarnings("unchecked")
  private void registerRecognizerInternal(Class<?> clazz, Supplier<?> recognizer) {
    this.recognizers.put(clazz, (Supplier<Recognizer<?>>) recognizer);
  }

  public Set<Map.Entry<Class<?>, Supplier<Recognizer<?>>>> getAllRecognizers() {
    return this.recognizers.entrySet();
  }
}
