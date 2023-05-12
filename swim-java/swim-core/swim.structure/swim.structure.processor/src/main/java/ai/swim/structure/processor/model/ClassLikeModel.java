package ai.swim.structure.processor.model;

import ai.swim.structure.annotations.AutoForm;
import ai.swim.structure.processor.writer.Writer;

import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClassLikeModel extends StructuralModel {
  private final List<FieldModel> fields;
  private final List<ExecutableElement> methods;
  private boolean isAbstract;

  public ClassLikeModel(TypeMirror type, TypeElement root, PackageElement declaredPackage) {
    super(type, root, declaredPackage);
    this.fields = new ArrayList<>();
    this.methods = new ArrayList<>();
  }

  public boolean isAbstract() {
    return isAbstract;
  }

  public void setAbstract(boolean isAbstract) {
    this.isAbstract = isAbstract;
  }

  public List<ExecutableElement> getMethods() {
    return methods;
  }

  public void addMethod(ExecutableElement method) {
    this.methods.add(method);
  }

  public List<FieldModel> getFields() {
    return fields;
  }

  public FieldModel getFieldByPropertyName(String propertyName) {
    for (FieldModel field : fields) {
      if (field.propertyName().equals(propertyName)) {
        return field;
      }
    }

    return null;
  }

  public void addField(FieldModel field) {
    this.fields.add(field);
  }

  public void merge(ClassLikeModel with) {
    this.methods.addAll(with.methods);
    this.fields.addAll(with.fields);
  }

  @Override
  public void write(Writer writer) throws IOException {
    writer.writeClass(this);
  }

  public String getTag() {
    AutoForm.Tag tag = getElement().getAnnotation(AutoForm.Tag.class);

    if (tag == null || tag.value().isBlank()) {
      return getJavaClassName();
    } else {
      return tag.value();
    }
  }

  public String getJavaClassName() {
    return getElement().getSimpleName().toString();
  }

  @Override
  public boolean isClassLike() {
    return true;
  }

  public boolean isClass() {
    return getElement().getKind() == ElementKind.CLASS;
  }

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

  public Name qualifiedName() {
    return getElement().getQualifiedName();
  }

  public List<? extends TypeParameterElement> getTypeParameters() {
    return getElement().getTypeParameters();
  }
}
