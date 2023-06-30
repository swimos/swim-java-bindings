package ai.swim.server.lanes.value;

import ai.swim.server.lanes.lifecycle.OnEvent;
import ai.swim.server.lanes.lifecycle.OnSet;
import ai.swim.structure.Form;

public final class ValueLaneView<T> implements ValueLane<T> {
  private OnSet<T> onSet;
  private OnEvent<T> onEvent;
  private final Form<T> form;

  public ValueLaneView(Form<T> form) {
    this.form = form;
  }

  @Override
  public Form<T> valueForm() {
    return form;
  }

  @Override
  public ValueLaneView<T> onSet(OnSet<T> onSet) {
    this.onSet = onSet;
    return this;
  }

  @Override
  public ValueLaneView<T> onEvent(OnEvent<T> onEvent) {
    this.onEvent = onEvent;
    return this;
  }

  @Override
  public void trace(Object message) {
    throw new AssertionError();
  }

  @Override
  public void debug(Object message) {
    throw new AssertionError();
  }

  @Override
  public void info(Object message) {
    throw new AssertionError();
  }

  @Override
  public void warn(Object message) {
    throw new AssertionError();
  }

  @Override
  public void error(Object message) {
    throw new AssertionError();
  }

  @Override
  public void fail(Object message) {
    throw new AssertionError();
  }

  public Form<T> getForm() {
    return form;
  }
}
