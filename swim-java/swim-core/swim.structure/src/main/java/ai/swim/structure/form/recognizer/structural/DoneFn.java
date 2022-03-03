package ai.swim.structure.form.recognizer.structural;

import ai.swim.structure.form.Builder;

@FunctionalInterface
public interface DoneFn<T> {
  T onDone(Builder<T> builder) throws RuntimeException;
}
