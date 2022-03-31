package ai.swim.structure;

import ai.swim.recon.event.ReadEvent;

public interface RecognizingBuilder<I> {

  default boolean feedIndexed(int index, ReadEvent event) {
    throw new UnsupportedOperationException();
  }

  default boolean feed(ReadEvent event) {
    throw new UnsupportedOperationException();
  }

  I bind();

}
