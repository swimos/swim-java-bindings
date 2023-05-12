package ai.swim.structure.processor.model;

import ai.swim.structure.processor.writer.Writable;

import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.List;

public abstract class StructuralModel extends Model implements Writable {
  protected List<Model> subTypes;

  public StructuralModel(TypeMirror type, Element element, PackageElement packageElement) {
    super(type, element, packageElement);
    this.subTypes = new ArrayList<>();
  }

  @Override
  public TypeElement getElement() {
    return (TypeElement) super.getElement();
  }

  @Override
  public InitializedType instantiate(TypeInitializer initializer, boolean inConstructor) {
    return null;
  }

  @Override
  public String toString() {
    return null;
  }

  public List<Model> getSubTypes() {
    return subTypes;
  }

  public void setSubTypes(List<Model> subTypes) {
    this.subTypes = subTypes;
  }


}
