package ai.swim.structure.processor.model.mapping;

import ai.swim.structure.processor.model.InitializedType;
import ai.swim.structure.processor.model.Model;
import ai.swim.structure.processor.model.TypeInitializer;

import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.type.DeclaredType;

/***
 * A java.util.Map model.
 */
public class MapLibraryModel extends KnownTypeModel {
  /**
   * A model corresponding to the map's key type.
   */
  private final Model keyModel;

  /**
   * A model corresponding to the map's value type.
   */
  private final Model valueModel;

  /**
   * Constructs a new map library model.
   *
   * @param typedContainer a declared type of the map. I.e, {@code Map<String,Integer>}. Any type unrolling must have
   *                       already been performed on the type. I.e, {@code Map<? extends CharSequence, ? extends Number>}
   *                       must have already been unrolled to {@code Map<CharSequence, Number>}.
   * @param element        the root element of this model.
   * @param keyModel       a model corresponding to the map's key type.
   * @param valueModel     a model corresponding to the map's value type.
   * @param packageElement java.util.Map.
   */
  public MapLibraryModel(DeclaredType typedContainer, Element element, Model keyModel, Model valueModel, PackageElement packageElement) {
    super(typedContainer, element, packageElement, TypeMapping.Map);
    this.keyModel = keyModel;
    this.valueModel = valueModel;
  }

  @Override
  public InitializedType instantiate(TypeInitializer initializer, boolean inConstructor) {
    return initializer.declared(this, inConstructor, keyModel, valueModel);
  }

  @Override
  public String toString() {
    return "MapLibraryModel{" +
            "keyModel=" + keyModel +
            ", valueModel=" + valueModel +
            ", type=" + type +
            '}';
  }
}
