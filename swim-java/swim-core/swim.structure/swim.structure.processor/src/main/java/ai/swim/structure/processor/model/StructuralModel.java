package ai.swim.structure.processor.model;

import ai.swim.structure.annotations.AutoForm;
import ai.swim.structure.processor.writer.Writable;

import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.List;

/**
 * An abstract structural model that provides the common properties and operations for class-like and interface models.
 */
public abstract class StructuralModel extends Model implements Writable {
  /**
   * A list of subtypes from this structural model.
   */
  protected List<Model> subTypes;

  public StructuralModel(TypeMirror type, Element element, PackageElement packageElement) {
    super(type, element, packageElement);
    this.subTypes = new ArrayList<>();
  }

  @Override
  public TypeElement getElement() {
    return (TypeElement) super.getElement();
  }

  @Override
  public InitializedType instantiate(TypeInitializer initializer, boolean inConstructor) {
    return null;
  }

  @Override
  public String toString() {
    return null;
  }

  /**
   * Returns a list of subtypes of this model.
   */
  public List<Model> getSubTypes() {
    return subTypes;
  }

  /**
   * Sets a list of subtypes for this model.
   */
  public void setSubTypes(List<Model> subTypes) {
    this.subTypes = subTypes;
  }

  /**
   * Returns a String representation of this class's tag. Either the value from {@code @Tag} or the simple Java class
   * name.
   */
  public String getTag() {
    AutoForm.Tag tag = getElement().getAnnotation(AutoForm.Tag.class);

    if (tag == null || tag.value().isBlank()) {
      return getJavaClassName();
    } else {
      return tag.value();
    }
  }

  /**
   * Returns this class element's simple name.
   */
  public String getJavaClassName() {
    return getElement().getSimpleName().toString();
  }

}
