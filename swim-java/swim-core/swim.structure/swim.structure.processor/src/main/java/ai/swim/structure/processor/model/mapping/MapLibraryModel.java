package ai.swim.structure.processor.model.mapping;

import ai.swim.structure.processor.model.InitializedType;
import ai.swim.structure.processor.model.Model;
import ai.swim.structure.processor.model.TypeInitializer;

import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.type.DeclaredType;

public class MapLibraryModel extends KnownTypeModel {
  private final Model keyModel;
  private final Model valueModel;

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
