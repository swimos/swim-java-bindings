package ai.swim.structure.processor.recognizer;

import ai.swim.structure.annotations.AutoForm;
import ai.swim.structure.processor.context.ScopedContext;
import ai.swim.structure.processor.inspect.FieldView;
import ai.swim.structure.processor.schema.FieldModel;
import com.squareup.javapoet.CodeBlock;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ClassMap extends StructuralRecognizer {

  private final TypeElement root;
  private final List<FieldModel> memberVariables;
  private final List<ExecutableElement> methods;
  private final PackageElement declaredPackage;
  private List<RecognizerModel> subTypes;
  private boolean isAbstract;

  public ClassMap(TypeElement root, PackageElement declaredPackage) {
    super(root.asType());
    this.root = root;
    this.memberVariables = new ArrayList<>();
    this.methods = new ArrayList<>();
    this.declaredPackage = declaredPackage;
    this.subTypes = new ArrayList<>();
  }

  public String recognizerName() {
    return this.getJavaClassName() + "Recognizer";
  }

  public String canonicalRecognizerName() {
    return String.format("%s.%s", this.declaredPackage.getQualifiedName().toString(), this.recognizerName());
  }

  @Override
  public CodeBlock initializer(ScopedContext context,boolean inConstructor) {
    return CodeBlock.of("new $L()", this.canonicalRecognizerName());
  }

  public FieldView getFieldViewByPropertyName(String propertyName) {
    for (FieldModel memberVariable : this.memberVariables) {
      if (memberVariable.propertyName().equals(propertyName)) {
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

    if (autoForm.value().isBlank()) {
      return getJavaClassName();
    } else {
      return autoForm.value();
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
        ", memberVariables=" + memberVariables +
        ", methods=" + methods +
        ", declaredPackage=" + declaredPackage +
        ", subTypes=" + subTypes +
        '}';
  }

  public void addField(FieldModel field) {
    this.memberVariables.add(field);
  }

  public void addMethod(ExecutableElement method) {
    this.methods.add(method);
  }

  /**
   * Returns the root element representing either a class, interface or enumeration.
   * <p>
   * If this is a class, then the kind of the element is guaranteed to be ElementKind#Class and the TypeMirror is
   * guaranteed to be a DeclaredType.
   */
  public TypeElement getRoot() {
    return root;
  }

  @Override
  public TypeMirror type(ProcessingEnvironment environment) {
    return root.asType();
  }

  public List<FieldModel> getFieldModels() {
    return this.memberVariables;
  }

  public void setSubTypes(List<RecognizerModel> subTypes) {
    this.subTypes = subTypes;
  }

  public void setAbstract(boolean isAbstract) {
    this.isAbstract = isAbstract;
  }

  public boolean isAbstract() {
    return isAbstract;
  }

  public List<RecognizerModel> getSubTypes() {
    return subTypes;
  }
}
