package ai.swim.structure.recognizer.proxy;

import ai.swim.structure.recognizer.Recognizer;
import ai.swim.structure.recognizer.untyped.UntypedRecognizer;

public abstract class TypeParameter<T> {
  private static UntypedParameter UNTYPED_PARAMETER;

  public static <T> TypeParameter<T> forClass(Class<T> target) {
    return new GenericTypeParameter<>(target);
  }

  public static TypeParameter<Object> untyped() {
    if (UNTYPED_PARAMETER == null) {
      UNTYPED_PARAMETER = new UntypedParameter();
    }

    return UNTYPED_PARAMETER;
  }

  public abstract Recognizer<T> visit(RecognizerProxy proxy);
}

final class GenericTypeParameter<T> extends TypeParameter<T> {
  private final Class<T> target;

  GenericTypeParameter(Class<T> target) {
    this.target = target;
  }

  @Override
  public Recognizer<T> visit(RecognizerProxy proxy) {
    return proxy.lookupStructural(target);
  }
}

final class UntypedParameter extends TypeParameter<Object> {
  @Override
  public Recognizer<Object> visit(RecognizerProxy proxy) {
    return new UntypedRecognizer<>();
  }
}