package ai.swim.structure.processor.model;

import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.type.TypeMirror;

/**
 * An array type model. I.e, Integer[].
 */
public class ArrayLibraryModel extends Model {
  private final TypeMirror componentType;
  private final Model componentModel;

  /**
   * Constructs a array library model.
   *
   * @param type           the type of the array. I.e, Integer[]
   * @param element        the root element of this model.
   * @param componentType  the type of the array's elements. I.e, Integer
   * @param componentModel a model for the array's elements.
   * @param packageElement of the component type. I.e, java.lang.Integer.
   */
  public ArrayLibraryModel(TypeMirror type, Element element, TypeMirror componentType, Model componentModel, PackageElement packageElement) {
    super(type, element, packageElement);
    this.componentType = componentType;
    this.componentModel = componentModel;
  }

  @Override
  public InitializedType instantiate(TypeInitializer initializer, boolean inConstructor) {
    return initializer.arrayType(this, inConstructor);
  }

  /**
   * Returns a model for the array's elements.
   */
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
