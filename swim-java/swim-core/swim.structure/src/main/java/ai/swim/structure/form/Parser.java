package ai.swim.structure.form;

import ai.swim.structure.form.event.ReadEvent;
import swim.codec.Input;

public abstract class Parser {

  public boolean isCont() {
    return true;
  }

  public boolean isDone() {
    return false;
  }

  public boolean isError() {
    return false;
  }

  public abstract Parser feed(Input input);

  public ReadEvent bind() {
    throw new IllegalStateException();
  }

  public RuntimeException trap() {
    throw new IllegalStateException();
  }

}
