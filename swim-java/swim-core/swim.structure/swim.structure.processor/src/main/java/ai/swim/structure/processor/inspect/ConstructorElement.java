package ai.swim.structure.processor.inspect;

import javax.lang.model.element.ExecutableElement;

public class ConstructorElement {
  private final ExecutableElement constructor;

  public ConstructorElement(ExecutableElement constructor) {
    this.constructor = constructor;
  }

  @Override
  public String toString() {
    return "ConstructorElement{" +
        "constructor=" + constructor +
        '}';
  }

  public ExecutableElement getElement() {
    return constructor;
  }
}
