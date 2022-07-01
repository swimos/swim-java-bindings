package ai.swim.structure.processor.context;

import javax.lang.model.element.PackageElement;

public class NameFactory {
  private final String name;
  private final PackageElement packageElement;

  public NameFactory(String name, PackageElement packageElement) {
    this.name = name;
    this.packageElement = packageElement;
  }

  public String getName() {
    return name;
  }

  public String builderClassName() {
    return String.format("%sBuilder", this.name);
  }

  public String headerBuilderClassName() {
    return String.format("%sHeaderBuilder", this.name);
  }

  public String headerBuilderCanonicalName() {
    return String.format("%s.%s.%s", this.packageElement.getQualifiedName(), this.recognizerClassName(), this.headerBuilderClassName());
  }

  public String headerClassName() {
    return String.format("%sHeader", this.name);
  }

  public String headerCanonicalName() {
    return String.format("%s.%s.%sHeader", this.packageElement.getQualifiedName(), this.recognizerClassName(), this.name);
  }

  public String headerBuilderFieldName() {
    return "headerBuilder";
  }

  public String fieldBuilderName(String fieldName) {
    return String.format("%sBuilder", fieldName);
  }

  public String headerBuilderMethod() {
    return "ai.swim.structure.recognizer.structural.delegate.HeaderRecognizer.headerBuilder";
  }

  public String recognizerClassName() {
    return String.format("%sRecognizer", this.name);
  }

  public String concreteRecognizerClassName() {
    return String.format("%sConcreteRecognizer", this.name);
  }

  public String typeParameterName(String gen) {
    if (gen.startsWith("<")) {
      gen = gen.replace("<", "");
    }
    if (gen.startsWith(">")) {
      gen = gen.replace(">", "");
    }

    return String.format("%sType", gen.toLowerCase());
  }
}
