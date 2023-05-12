package ai.swim.structure.processor.model;

import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.type.TypeMirror;

public class UntypedModel extends Model {
  public UntypedModel(TypeMirror typeMirror, Element element, PackageElement packageElement) {
    super(typeMirror, element, packageElement);
  }

  @Override
  public InitializedType instantiate(TypeInitializer initializer, boolean inConstructor) throws InvalidModelException {
    return initializer.untyped(type, inConstructor);
  }

  @Override
  public String toString() {
    return "UntypedModel{" +
            "type=" + type +
            '}';
  }
}
