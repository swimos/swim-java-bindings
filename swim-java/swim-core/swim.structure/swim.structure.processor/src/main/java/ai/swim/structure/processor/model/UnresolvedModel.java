package ai.swim.structure.processor.model;

import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.type.TypeMirror;

public class UnresolvedModel extends Model {
  public UnresolvedModel(TypeMirror type, Element element, PackageElement packageElement) {
    super(type, element, packageElement);
  }

  @Override
  public boolean isUnresolved() {
    return true;
  }

  @Override
  public InitializedType instantiate(TypeInitializer initializer, boolean inConstructor) {
    return initializer.unresolved(this, inConstructor);
  }

  @Override
  public String toString() {
    return "UnresolvedModel{" +
            "type=" + type +
            '}';
  }
}
