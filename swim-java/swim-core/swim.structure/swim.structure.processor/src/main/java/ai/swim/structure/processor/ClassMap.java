package ai.swim.structure.processor;

import ai.swim.structure.processor.structure.ConstructorElement;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import java.util.ArrayList;
import java.util.List;

public class ClassMap {
  private final Element root;
  private final ConstructorElement constructor;
  private final List<FieldView> memberVariables;
  private final List<ExecutableElement> methods;

  public ClassMap(Element root, ConstructorElement constructor) {
    this.root = root;
    this.constructor = constructor;
    this.memberVariables = new ArrayList<>();
    this.methods = new ArrayList<>();
  }

  public FieldView getField(String name) {
    for (FieldView memberVariable : this.memberVariables) {
      if (memberVariable.getName().contentEquals(name)) {
        return memberVariable;
      }
    }

    return null;
  }

  public ExecutableElement getMethod(String name) {
    for (ExecutableElement method : this.methods) {
      if (method.getSimpleName().contentEquals(name)) {
        return method;
      }
    }

    return null;
  }

  public List<ExecutableElement> getMethods() {
    return methods;
  }

  public List<FieldView> getFields() {
    return memberVariables;
  }

  public ConstructorElement getConstructor() {
    return this.constructor;
  }

  public void merge(ClassMap with) {
    this.methods.addAll(with.methods);
    this.memberVariables.addAll(with.memberVariables);
  }

  @Override
  public String toString() {
    return "ElementMap{" +
        "root=" + root +
        ", constructor=" + constructor +
        ", memberVariables=" + memberVariables +
        ", methods=" + methods +
        '}';
  }

  public void addField(FieldView field) {
    this.memberVariables.add(field);
  }

  public void addMethod(ExecutableElement method) {
    this.methods.add(method);
  }

  /**
   * Returns the root element representing either a class or enumeration.
   *
   * If this is a class, then the kind of the element is guaranteed to be ElementKind#Class and the TypeMirror is
   * guaranteed to be a DeclaredType.
   */
  public Element getRoot() {
    return root;
  }

}
