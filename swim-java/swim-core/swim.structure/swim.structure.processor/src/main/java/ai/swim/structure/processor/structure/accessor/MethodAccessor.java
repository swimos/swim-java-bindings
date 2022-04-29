package ai.swim.structure.processor.structure.accessor;

import javax.lang.model.element.ExecutableElement;

public class MethodAccessor extends Accessor {
  private final ExecutableElement method;

  public MethodAccessor(ExecutableElement method) {
    this.method = method;
  }

  @Override
  public void write(Object with, Object var) {

  }

  @Override
  public String toString() {
    return "MethodAccessor{" +
        "method=" + method +
        '}';
  }
}
