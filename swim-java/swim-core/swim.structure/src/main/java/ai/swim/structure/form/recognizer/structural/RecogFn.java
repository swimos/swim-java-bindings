package ai.swim.structure.form.recognizer.structural;

import ai.swim.structure.form.Builder;
import ai.swim.structure.form.event.ReadEvent;

@FunctionalInterface
public interface RecogFn<T> {
  boolean selectRecognize(Builder<T> builder, int index, ReadEvent event) throws RuntimeException;
}
