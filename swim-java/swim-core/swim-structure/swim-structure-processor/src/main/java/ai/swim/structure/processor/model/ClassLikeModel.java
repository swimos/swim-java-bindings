package ai.swim.structure.processor.model;

import ai.swim.structure.processor.writer.Writer;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A class-like model that may represent a concrete or abstract class or an enumeration.
 */
public class ClassLikeModel extends StructuralModel {
  /**
   * Resolved fields that this class-like model contains.
   */
  private final List<FieldModel> fields;
  /**
   * Methods that this class-like model contains for field access.
   */
  private final List<ExecutableElement> methods;
  /**
   * Whether this class-like model is abstract.
   */
  private boolean isAbstract;

  public ClassLikeModel(TypeMirror type, TypeElement root, PackageElement declaredPackage) {
    super(type, root, declaredPackage);
    this.fields = new ArrayList<>();
    this.methods = new ArrayList<>();
  }

  /**
   * Returns whether this class-like model is abstract.
   */
  public boolean isAbstract() {
    return isAbstract;
  }

  /**
   * Sets whether this class-like model is abstract.
   */
  public void setAbstract(boolean isAbstract) {
    this.isAbstract = isAbstract;
  }

  /**
   * Returns a list of all the methods that this class-like model contains.
   * <p>
   * Methods are for field access only.
   */
  public List<ExecutableElement> getMethods() {
    return methods;
  }

  /**
   * Inserts a new method.
   */
  public void addMethod(ExecutableElement method) {
    this.methods.add(method);
  }

  /**
   * Returns a list of the fields that this class-like model contains.
   */
  public List<FieldModel> getFields() {
    return fields;
  }

  /**
   * Looks up a field by its property name; the name of the field that is used for serialization and deserialization.
   *
   * @param propertyName the name of the field.
   * @return either the matching field or null.
   */
  public FieldModel getFieldByPropertyName(String propertyName) {
    for (FieldModel field : fields) {
      if (field.propertyName().equals(propertyName)) {
        return field;
      }
    }

    return null;
  }

  /**
   * Inserts a new field.
   */
  public void addField(FieldModel field) {
    this.fields.add(field);
  }

  /**
   * Merges the fields from {@code with} into this model.
   */
  public void merge(ClassLikeModel with) {
    this.methods.addAll(with.methods);
    this.fields.addAll(with.fields);
  }

  @Override
  public void write(Writer writer) throws IOException {
    writer.writeClass(this);
  }

  @Override
  public boolean isClassLike() {
    return true;
  }

  /**
   * Returns whether this class-like model is a *class*.
   */
  public boolean isClass() {
    return getElement().getKind() == ElementKind.CLASS;
  }

  /**
   * Returns whether this class-like model is a *enum*.
   */
  public boolean isEnum() {
    return getElement().getKind() == ElementKind.ENUM;
  }

  @Override
  public InitializedType instantiate(TypeInitializer initializer, boolean inConstructor) {
    return initializer.declared(this, inConstructor);
  }

  @Override
  public String toString() {
    return "ClassLikeModel{" +
        "element=" + getElement() +
        ", declaredPackage=" + getDeclaredPackage() +
        ", fields=" + fields +
        ", methods=" + methods +
        ", subTypes=" + subTypes +
        ", isAbstract=" + isAbstract +
        '}';
  }

  /**
   * Returns this class element's qualified (canonical) name.
   */
  public Name qualifiedName() {
    return getElement().getQualifiedName();
  }

  /**
   * Returns this class element's type parameters, if any.
   */
  public List<? extends TypeParameterElement> getTypeParameters() {
    return getElement().getTypeParameters();
  }
}
