package ai.swim.structure.recognizer.proxy;


import ai.swim.structure.annotations.AutoloadedRecognizer;
import ai.swim.structure.recognizer.Recognizer;
import ai.swim.structure.recognizer.RecognizerException;
import ai.swim.structure.recognizer.SimpleRecognizer;
import ai.swim.structure.recognizer.std.MapRecognizer;
import ai.swim.structure.recognizer.std.ScalarRecognizer;
import ai.swim.structure.recognizer.std.collections.ListRecognizer;
import ai.swim.structure.recognizer.structural.StructuralRecognizer;
import ai.swim.structure.recognizer.untyped.UntypedRecognizer;
import ai.swim.structure.recognizer.value.ValueRecognizer;
import ai.swim.structure.value.Value;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;
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
    recognizers.put(
        Integer.class,
        RecognizerFactory.buildFrom(Integer.class, SimpleRecognizer.class, () -> ScalarRecognizer.INTEGER));
    recognizers.put(
        Long.class,
        RecognizerFactory.buildFrom(Long.class, SimpleRecognizer.class, () -> ScalarRecognizer.LONG));
    recognizers.put(
        byte[].class,
        RecognizerFactory.buildFrom(byte[].class, SimpleRecognizer.class, () -> ScalarRecognizer.BLOB));
    recognizers.put(
        Boolean.class,
        RecognizerFactory.buildFrom(Boolean.class, SimpleRecognizer.class, () -> ScalarRecognizer.BOOLEAN));
    recognizers.put(
        Float.class,
        RecognizerFactory.buildFrom(Float.class, SimpleRecognizer.class, () -> ScalarRecognizer.FLOAT));
    recognizers.put(
        Double.class,
        RecognizerFactory.buildFrom(Double.class, SimpleRecognizer.class, () -> ScalarRecognizer.DOUBLE));
    recognizers.put(
        Integer.TYPE,
        RecognizerFactory.buildFrom(Integer.TYPE, SimpleRecognizer.class, () -> ScalarRecognizer.INTEGER));
    recognizers.put(
        Long.TYPE,
        RecognizerFactory.buildFrom(Long.TYPE, SimpleRecognizer.class, () -> ScalarRecognizer.LONG));
    recognizers.put(
        Boolean.TYPE,
        RecognizerFactory.buildFrom(Boolean.TYPE, SimpleRecognizer.class, () -> ScalarRecognizer.BOOLEAN));
    recognizers.put(
        Float.TYPE,
        RecognizerFactory.buildFrom(Float.TYPE, SimpleRecognizer.class, () -> ScalarRecognizer.FLOAT));
    recognizers.put(
        Double.TYPE,
        RecognizerFactory.buildFrom(Double.TYPE, SimpleRecognizer.class, () -> ScalarRecognizer.DOUBLE));
    recognizers.put(
        String.class,
        RecognizerFactory.buildFrom(String.class, SimpleRecognizer.class, () -> ScalarRecognizer.STRING));
    recognizers.put(
        Object.class,
        RecognizerFactory.buildFrom(Object.class, UntypedRecognizer.class, UntypedRecognizer::new));
    recognizers.put(
        BigDecimal.class,
        RecognizerFactory.buildFrom(BigDecimal.class,
                                    SimpleRecognizer.class,
                                    () -> ScalarRecognizer.BIG_DECIMAL));
    recognizers.put(
        BigInteger.class,
        RecognizerFactory.buildFrom(BigInteger.class,
                                    SimpleRecognizer.class,
                                    () -> ScalarRecognizer.BIG_INTEGER));
    recognizers.put(
        Number.class,
        RecognizerFactory.buildFrom(Number.class, SimpleRecognizer.class, () -> ScalarRecognizer.NUMBER));
    recognizers.put(Map.class, RecognizerFactory.buildFrom(Map.class, MapRecognizer.class, null));
    recognizers.put(Collection.class, RecognizerFactory.buildFrom(Collection.class, ListRecognizer.class, null));
    recognizers.put(
        Void.class,
        RecognizerFactory.buildFrom(Void.class, SimpleRecognizer.class, () -> ScalarRecognizer.VOID));
    recognizers.put(Value.class, RecognizerFactory.buildFrom(Value.class, ValueRecognizer.class, ValueRecognizer::new));

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
        String error = String.format(
            "%s is annotated with @%s(%s.class) but %s does not extend %s",
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
            throw new RuntimeException(String.format(
                "Failed to created a new instance of recognizer '%s'",
                clazz.getCanonicalName()), e);
          }
        };

        // safety: checked that the class is assignable above
        Class<Recognizer<?>> typedClass = (Class<Recognizer<?>>) clazz;
        recognizers.put(targetClass, RecognizerFactory.buildFromAny(clazz, typedClass, supplier));
      } catch (NoSuchMethodException e) {
        throw new RuntimeException(String.format(
            "Recognizer '%s' does not contain a zero-arg constructor",
            clazz.getCanonicalName()), e);
      }
    }
  }

  public static RecognizerProxy getProxy() {
    return RecognizerProxy.INSTANCE;
  }

  public <T> Recognizer<T> lookup(Class<T> clazz, RecognizerTypeParameter<?>... typeParameters) {
    if (clazz == null) {
      throw new NullPointerException();
    }

    if (typeParameters != null && typeParameters.length != 0) {
      return lookupTyped(clazz, typeParameters);
    } else {
      return lookupUntyped(clazz);
    }
  }

  @SuppressWarnings("unchecked")
  private <T> Recognizer<T> lookupUntyped(Class<T> clazz) {
    RecognizerFactory<T> recognizerSupplier = (RecognizerFactory<T>) this.recognizers.get(clazz);

    if (Value.class.isAssignableFrom(clazz)) {
      return (Recognizer<T>) new ValueRecognizer();
    }

    if (recognizerSupplier == null) {
      throw new RecognizerException("No recognizer found for class: " + clazz);
    }

    return recognizerSupplier.newInstance();
  }

  @SuppressWarnings("unchecked")
  public <T> StructuralRecognizer<T> lookupTyped(Class<T> clazz, RecognizerTypeParameter<?>... typeParameters) {
    RecognizerFactory<T> factory = (RecognizerFactory<T>) this.recognizers.get(clazz);

    if (factory == null) {
      return fromStdClass(clazz, typeParameters);
    }

    if (!factory.isStructural()) {
      return null;
    }

    return (StructuralRecognizer<T>) factory.newTypedInstance(typeParameters);
  }

  @SuppressWarnings("unchecked")
  private <T> StructuralRecognizer<T> fromStdClass(Class<T> clazz, RecognizerTypeParameter<?>... typeParameters) {
    if (Map.class.isAssignableFrom(clazz)) {
      return (StructuralRecognizer<T>) lookupTyped(Map.class, typeParameters);
    }

    if (Collection.class.isAssignableFrom(clazz)) {
      return (StructuralRecognizer<T>) lookupTyped(Collection.class, typeParameters);
    }

    throw new RecognizerException("No recognizer found for class: " + clazz);
  }

}
