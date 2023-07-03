package ai.swim.structure;

import ai.swim.recon.event.ReadEvent;

public interface RecognizingBuilder<T> {

  default boolean feedIndexed(int index, ReadEvent event) {
    throw new UnsupportedOperationException();
  }

  default boolean feed(ReadEvent event) {
    throw new UnsupportedOperationException();
  }

  T bind();

  default T bindOr(T defaultValue) {
    throw new UnsupportedOperationException();
  }

  RecognizingBuilder<T> reset();
}
