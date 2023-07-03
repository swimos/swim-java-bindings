package ai.swim.structure.processor.model;

import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.type.TypeMirror;
import java.util.Arrays;

/**
 * A model representing a known class type and for resolving known types; primitives, array types, list and map types.
 */
public class ParameterisedTypeModel extends Model {

  /**
   * An enumeration over {@link ParameterisedTypeModel} kinds for discrimination.
   */
  public enum Mapping {
    /**
     * java.util.List
     */
    List,
    /**
     * java.util.Map
     */
    Map
  }

  private final Mapping typeMapping;
  private final Model[] typeModels;

  public ParameterisedTypeModel(TypeMirror mirror,
                                Element element,
                                PackageElement packageElement,
                                Mapping typeMapping,
                                Model... typeModels) {
    super(mirror, element, packageElement);
    this.typeMapping = typeMapping;
    this.typeModels = typeModels;
  }

  @Override
  public boolean isParameterisedType() {
    return true;
  }

  @Override
  public InitializedType instantiate(TypeInitializer initializer, boolean inConstructor) {
    return initializer.declared(this, inConstructor, typeModels);
  }

  /**
   * Returns the type that this known type model references.
   */
  public Mapping getTypeMapping() {
    return typeMapping;
  }

  @Override
  public String toString() {
    return "ParameterisedTypeModel{" +
        "typeMapping=" + typeMapping +
        ", typeModels=" + Arrays.toString(typeModels) +
        '}';
  }
}
