package ai.swim.structure.recognizer.proxy;


import ai.swim.structure.annotations.AutoloadedRecognizer;
import ai.swim.structure.recognizer.Recognizer;
import ai.swim.structure.recognizer.SimpleRecognizer;
import ai.swim.structure.recognizer.std.ScalarRecognizer;
import ai.swim.structure.recognizer.structural.StructuralRecognizer;
import ai.swim.structure.recognizer.untyped.UntypedRecognizer;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class RecognizerProxy {

  private static final RecognizerProxy INSTANCE = new RecognizerProxy();
  private final ConcurrentHashMap<Class<?>, RecognizerFactory<?>> recognizers;

  private RecognizerProxy() {
    recognizers = loadRecognizers();
  }

  @SuppressWarnings("unchecked")
  private static ConcurrentHashMap<Class<?>, RecognizerFactory<?>> loadRecognizers() {
    ConcurrentHashMap<Class<?>, RecognizerFactory<?>> recognizers = new ConcurrentHashMap<>();
    recognizers.put(Integer.class, RecognizerFactory.buildFrom(Integer.class, SimpleRecognizer.class, () -> ScalarRecognizer.INTEGER));
    recognizers.put(Long.class, RecognizerFactory.buildFrom(Long.class, SimpleRecognizer.class, () -> ScalarRecognizer.LONG));
    recognizers.put(byte[].class, RecognizerFactory.buildFrom(byte[].class, SimpleRecognizer.class, () -> ScalarRecognizer.BLOB));
    recognizers.put(Boolean.class, RecognizerFactory.buildFrom(Boolean.class, SimpleRecognizer.class, () -> ScalarRecognizer.BOOLEAN));
    recognizers.put(String.class, RecognizerFactory.buildFrom(String.class, SimpleRecognizer.class, () -> ScalarRecognizer.STRING));
    recognizers.put(Object.class, RecognizerFactory.buildFrom(Object.class, UntypedRecognizer.class, UntypedRecognizer::new));
    recognizers.put(BigDecimal.class, RecognizerFactory.buildFrom(BigDecimal.class, SimpleRecognizer.class, () -> ScalarRecognizer.BIG_DECIMAL));
    recognizers.put(BigInteger.class, RecognizerFactory.buildFrom(BigInteger.class, SimpleRecognizer.class, () -> ScalarRecognizer.BIG_INTEGER));

    loadFromClassPath(recognizers);

    return recognizers;
  }

  @SuppressWarnings("unchecked")
  private static void loadFromClassPath(ConcurrentHashMap<Class<?>, RecognizerFactory<?>> recognizers) {
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
        Supplier<Recognizer<?>> supplier = () -> {
          try {
            return (Recognizer<?>) constructor.newInstance();
          } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(String.format("Failed to created a new instance of recognizer '%s'", clazz.getCanonicalName()), e);
          }
        };

        // safety: checked that the class is assignable above
        Class<Recognizer<?>> typedClass = (Class<Recognizer<?>>) clazz;
        recognizers.put(targetClass, RecognizerFactory.buildFromAny(targetClass, typedClass, supplier));
      } catch (NoSuchMethodException e) {
        throw new RuntimeException(String.format("Recognizer '%s' does not contain a zero-arg constructor", clazz.getCanonicalName()), e);
      }
    }
  }

  public static RecognizerProxy getInstance() {
    return RecognizerProxy.INSTANCE;
  }

  public <T> Recognizer<T> lookup(Class<T> clazz) {
    if (clazz == null) {
      throw new NullPointerException();
    }

    return getRecognizer(clazz);
  }

  @SuppressWarnings("unchecked")
  private <T> Recognizer<T> getRecognizer(Class<T> clazz) {
    if (clazz == null) {
      throw new NullPointerException();
    }

    RecognizerFactory<T> recognizerSupplier = (RecognizerFactory<T>) this.recognizers.get(clazz);
    return recognizerSupplier.newInstance();
  }

  public <C> StructuralRecognizer<C> lookupStructural(Class<C> clazz) {
    if (clazz == null) {
      throw new NullPointerException();
    }

    Recognizer<C> recognizer = lookup(clazz);
    if (recognizer instanceof StructuralRecognizer<C>) {
      return (StructuralRecognizer<C>) recognizer;
    } else {
      throw new ClassCastException(String.format("Recognizer for %s is not a structural recognizer", clazz));
    }
  }

  @SuppressWarnings("unchecked")
  public <T> StructuralRecognizer<T> lookupStructural(Class<T> clazz, TypeParameter<?>... typeParameters) {
    RecognizerFactory<T> factory = (RecognizerFactory<T>) this.recognizers.get(clazz);

    if (factory == null) {
      return null;
    }

    if (!factory.isStructural()) {
      return null;
    }

    if (typeParameters.length == 0) {
      return (StructuralRecognizer<T>) factory.newInstance();
    } else {
      return (StructuralRecognizer<T>) factory.newTypedInstance(this, typeParameters);
    }
  }

}
