package ai.swim.structure.recognizer.proxy;

import ai.swim.structure.annotations.AutoForm;
import ai.swim.structure.recognizer.Recognizer;
import ai.swim.structure.recognizer.RecognizerException;
import ai.swim.structure.recognizer.structural.StructuralRecognizer;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Supplier;

class RecognizerFactory<T> {
  private final Supplier<Recognizer<?>> supplier;
  private final Constructor<Recognizer<?>> typedConstructor;
  private final boolean isStructural;

  private RecognizerFactory(Supplier<Recognizer<?>> supplier, Constructor<Recognizer<?>> typedConstructor, boolean isStructural) {
    this.supplier = supplier;
    this.typedConstructor = typedConstructor;
    this.isStructural = isStructural;
  }

  @SuppressWarnings("unchecked")
  public static <T, R extends Recognizer<T>> RecognizerFactory<T> buildFrom(Class<T> targetClass, Class<R> recognizerClass, Supplier<Recognizer<?>> supplier) {
    Constructor<Recognizer<?>> typedConstructor = null;
    boolean isStructural = false;

    if (StructuralRecognizer.class.isAssignableFrom(recognizerClass)) {
      isStructural = true;

      for (Constructor<?> constructor : recognizerClass.getConstructors()) {
        if (constructor.getAnnotation(AutoForm.TypedConstructor.class) != null) {
          typedConstructor = (Constructor<Recognizer<?>>) constructor;
        }
      }
    }

    return new RecognizerFactory<>(supplier, typedConstructor, isStructural);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static RecognizerFactory buildFromAny(Class<?> targetClass, Class<? extends Recognizer> clazz, Supplier<Recognizer<?>> supplier) {
    return buildFrom(targetClass, clazz, supplier);
  }

  public boolean isStructural() {
    return isStructural;
  }

  @SuppressWarnings("unchecked")
  public Recognizer<T> newInstance() {
    return (Recognizer<T>) supplier.get();
  }

  @SuppressWarnings("unchecked")
  public Recognizer<T> newTypedInstance(TypeParameter<?>... typeParameters) {
    if (typedConstructor == null) {
      throw new IllegalStateException("Not a generic recognizer");
    } else {
      try {
        return (Recognizer<T>) typedConstructor.newInstance((Object[]) typeParameters);
      } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
        throw new RecognizerException(e);
      }
    }
  }

}
