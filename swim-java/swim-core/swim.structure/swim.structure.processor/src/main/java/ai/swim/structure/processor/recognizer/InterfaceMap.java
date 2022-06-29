package ai.swim.structure.processor.recognizer;

import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;

public class InterfaceMap extends StructuralRecognizer {

  private final Element root;
  private final PackageElement declaredPackage;
  private final List<StructuralRecognizer> subTypes;

  public InterfaceMap(Element root, PackageElement declaredPackage, List<StructuralRecognizer> subTypes) {
    this.root = root;
    this.declaredPackage = declaredPackage;
    this.subTypes = subTypes;
  }

  public String recognizerName() {
    return this.root.getSimpleName().toString() + "Recognizer";
  }

  public String canonicalRecognizerName() {
    return String.format("%s.%s", this.declaredPackage.getQualifiedName().toString(), this.recognizerName());
  }

  public List<StructuralRecognizer> getSubTypes() {
    return subTypes;
  }

  public PackageElement getDeclaredPackage() {
    return declaredPackage;
  }

  @Override
  public String recognizerInitializer() {
    return String.format("new %s()", this.canonicalRecognizerName());
  }

  @Override
  public TypeMirror type() {
    return root.asType();
  }

  public Element root() {
    return root;
  }
}
