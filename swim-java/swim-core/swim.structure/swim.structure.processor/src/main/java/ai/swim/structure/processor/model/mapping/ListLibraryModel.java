package ai.swim.structure.processor.model.mapping;

import ai.swim.structure.processor.model.InitializedType;
import ai.swim.structure.processor.model.Model;
import ai.swim.structure.processor.model.TypeInitializer;

import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.type.TypeMirror;

public class ListLibraryModel extends KnownTypeModel {
  private final Model elementModel;

  public ListLibraryModel(Model elementModel, Element element, TypeMirror typeMirror, PackageElement packageElement) {
    super(typeMirror, element, packageElement, TypeMapping.List);
    this.elementModel = elementModel;
  }

  @Override
  public InitializedType instantiate(TypeInitializer initializer, boolean inConstructor) {
    return initializer.declared(this, inConstructor, elementModel);
  }

  @Override
  public String toString() {
    return "ListLibraryModel{" +
            "elementModel=" + elementModel +
            ", type=" + type +
            '}';
  }
}
