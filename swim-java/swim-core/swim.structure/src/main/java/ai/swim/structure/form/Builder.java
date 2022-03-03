package ai.swim.structure.form;

import ai.swim.structure.form.event.ReadEvent;

public interface Builder<I> {
  boolean feed(Integer index, ReadEvent event);

  I bind();
}
