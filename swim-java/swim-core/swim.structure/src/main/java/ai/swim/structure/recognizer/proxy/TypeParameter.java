package ai.swim.structure.recognizer.proxy;

import ai.swim.structure.recognizer.Recognizer;
import ai.swim.structure.recognizer.untyped.UntypedRecognizer;

import java.util.function.Supplier;

public abstract class TypeParameter<T> {

  public static <T> TypeParameter<T> from(Class<T> target) {
    if (target == null) {
      return untyped();
    } else {
      return forClass(target);
    }
  }

  public static <T> TypeParameter<T> from(Supplier<Recognizer<T>> target) {
    if (target == null) {
      return untyped();
    } else {
      return resolved(target);
    }
  }

  public static <T> TypeParameter<T> forClass(Class<T> target) {
    return new GenericTypeParameter<>(target);
  }

  public static <T> TypeParameter<T> resolved(Supplier<Recognizer<T>> recognizer) {
    return new ResolvedParameter<>(recognizer);
  }

  public static <T> TypeParameter<T> untyped() {
    return new UntypedParameter<>();
  }

  public abstract Recognizer<T> build();
}

final class GenericTypeParameter<T> extends TypeParameter<T> {
  private final Class<T> target;

  GenericTypeParameter(Class<T> target) {
    this.target = target;
  }

  @Override
  public Recognizer<T> build() {
    return RecognizerProxy.getInstance().lookup(target);
  }
}

final class UntypedParameter<T> extends TypeParameter<T> {
  @Override
  public Recognizer<T> build() {
    return new UntypedRecognizer<>();
  }
}

final class ResolvedParameter<T> extends TypeParameter<T> {
  private final Supplier<Recognizer<T>> recognizer;

  ResolvedParameter(Supplier<Recognizer<T>> recognizer) {
    this.recognizer = recognizer;
  }

  @Override
  public Recognizer<T> build() {
    return this.recognizer.get();
  }
}