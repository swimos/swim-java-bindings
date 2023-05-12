package ai.swim.structure.processor.model.mapping;

import ai.swim.structure.processor.model.InitializedType;
import ai.swim.structure.processor.model.Model;
import ai.swim.structure.processor.model.TypeInitializer;

import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.type.TypeMirror;

/***
 * A java.util.List model.
 */
public class ListLibraryModel extends KnownTypeModel {
  /**
   * A model corresponding to the list's element type.
   */
  private final Model elementModel;

  /**
   * Constructs a new map library model.
   *
   * @param elementModel   a model corresponding to the list's element type.
   * @param element        the root element of this model.
   * @param typeMirror     a declared type of the list. I.e, {@code List<String>}. Any type unrolling must have already
   *                       been performed on the type. I.e, {@code List<? extends CharSequence>} must have already been
   *                       unrolled to {@code List<CharSequence>}.
   * @param packageElement java.util.List.
   */
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
