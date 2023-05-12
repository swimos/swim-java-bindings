package ai.swim.structure.processor.model;

import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.type.TypeMirror;

/**
 * A model that failed to be resolved by the {@link ModelInspector} and will require a runtime lookup.
 * <p>
 * A model will fail to be resolved if it has not been annotated with {@link ai.swim.structure.annotations.AutoForm}.
 * This will cause the model to be lookup at runtime for resolution.
 */
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
