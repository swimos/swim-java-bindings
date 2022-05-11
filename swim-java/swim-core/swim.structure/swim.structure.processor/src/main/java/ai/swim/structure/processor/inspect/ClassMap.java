package ai.swim.structure.processor.inspect;

import ai.swim.structure.annotations.AutoForm;
import ai.swim.structure.processor.recognizer.ClassRecognizerModel;
import ai.swim.structure.processor.schema.FieldModel;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ClassMap extends ClassRecognizerModel {

  private final Element root;
  private final ConstructorElement constructor;
  private final List<FieldModel> memberVariables;
  private final List<ExecutableElement> methods;
  private final PackageElement declaredPackage;

  public ClassMap(Element root, ConstructorElement constructor, PackageElement declaredPackage) {
    this.root = root;
    this.constructor = constructor;
    this.memberVariables = new ArrayList<>();
    this.methods = new ArrayList<>();
    this.declaredPackage = declaredPackage;
  }

  public FieldView getFieldView(String name) {
    for (FieldModel memberVariable : this.memberVariables) {
      if (memberVariable.getName().contentEquals(name)) {
        return memberVariable.getFieldView();
      }
    }

    return null;
  }

  public PackageElement getDeclaredPackage() {
    return declaredPackage;
  }

  public String getJavaClassName() {
    return this.root.getSimpleName().toString();
  }

  public String getTag() {
    AutoForm autoForm = this.root.getAnnotation(AutoForm.class);

    if (autoForm.tag().isBlank()) {
      return getJavaClassName();
    } else {
      return autoForm.tag();
    }
  }

  public List<ExecutableElement> getMethods() {
    return methods;
  }

  public List<FieldView> getFieldViews() {
    return memberVariables.stream().map(FieldModel::getFieldView).collect(Collectors.toList());
  }

  public void merge(ClassMap with) {
    this.methods.addAll(with.methods);
    this.memberVariables.addAll(with.memberVariables);
  }

  @Override
  public String toString() {
    return "ClassMap{" +
        "root=" + root +
        ", constructor=" + constructor +
        ", memberVariables=" + memberVariables +
        ", methods=" + methods +
        ", declaredPackage=" + declaredPackage +
        '}';
  }

  public void addField(FieldModel field) {
    this.memberVariables.add(field);
  }

  public void addMethod(ExecutableElement method) {
    this.methods.add(method);
  }

  /**
   * Returns the root element representing either a class or enumeration.
   * <p>
   * If this is a class, then the kind of the element is guaranteed to be ElementKind#Class and the TypeMirror is
   * guaranteed to be a DeclaredType.
   */
  public Element getRoot() {
    return root;
  }

  public String recognizerName() {
    return this.getJavaClassName() + "Recognizer";
  }

  public String canonicalRecognizerName() {
    return String.format("%s.%s", this.declaredPackage.getQualifiedName().toString(), this.recognizerName());
  }

  @Override
  public String recognizerInitializer() {
    return String.format("new %s()", this.canonicalRecognizerName());
  }

  public List<FieldModel> getFieldModels() {
    return this.memberVariables;
  }
}
