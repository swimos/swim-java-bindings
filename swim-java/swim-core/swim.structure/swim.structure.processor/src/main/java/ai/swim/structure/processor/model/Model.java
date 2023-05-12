package ai.swim.structure.processor.model;

import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.type.TypeMirror;

public abstract class Model {
  protected final TypeMirror type;
  protected final Element element;
  protected final PackageElement packageElement;

  public Model(TypeMirror type, Element element, PackageElement packageElement) {
    this.type = type;
    this.element = element;
    this.packageElement = packageElement;
  }

  public TypeMirror getType() {
    return type;
  }

  public Element getElement() {
    return element;
  }

  public PackageElement getDeclaredPackage() {
    return packageElement;
  }

  public boolean isClassLike() {
    return false;
  }

  public boolean isUnresolved() {
    return false;
  }

  public boolean isKnownType() {
    return false;
  }

  public abstract InitializedType instantiate(TypeInitializer initializer, boolean inConstructor);

  public Object defaultValue() {
    return null;
  }

  @Override
  public abstract String toString();
}
