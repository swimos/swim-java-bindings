package ai.swim.structure.processor.structure;

import javax.lang.model.element.ExecutableElement;

public class ConstructorElement implements Write{
  private final ExecutableElement constructor;

  public ConstructorElement(ExecutableElement constructor) {
    this.constructor = constructor;
  }

  @Override
  public void into(Object writer) {

  }

  @Override
  public String toString() {
    return "ConstructorElement{" +
        "constructor=" + constructor +
        '}';
  }
}
