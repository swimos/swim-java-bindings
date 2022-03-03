package ai.swim.structure.form.recognizer.structural;

import ai.swim.structure.form.Builder;

@FunctionalInterface
public interface ResetFn<T> {
  void reset(Builder<T> builder);
}
