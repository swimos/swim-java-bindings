package ai.swim.structure.processor.model;

import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.type.TypeMirror;

public class ArrayLibraryModel extends Model {
  private final TypeMirror componentType;
  private final Model componentModel;

  public ArrayLibraryModel(TypeMirror type, Element element, TypeMirror componentType, Model componentModel, PackageElement packageElement) {
    super(type, element, packageElement);
    this.componentType = componentType;
    this.componentModel = componentModel;
  }

  @Override
  public InitializedType instantiate(TypeInitializer initializer, boolean inConstructor) {
    return initializer.arrayType(this, inConstructor);
  }

  public Model getComponentModel() {
    return componentModel;
  }

  @Override
  public String toString() {
    return "ArrayLibraryModel{" +
            "arrayType=" + componentType +
            ", componentModel=" + componentModel +
            ", type=" + type +
            '}';
  }
}
