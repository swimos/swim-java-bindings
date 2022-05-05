package ai.swim.structure.processor.schema;

import javax.lang.model.element.Element;

public abstract class ElementSchema {

  private final Element mirror;

  protected ElementSchema(Element mirror) {
    this.mirror = mirror;
  }

  public static ElementSchema fromElement(Element element) {
    return null;
  }


  @Override
  public abstract String toString();
}
