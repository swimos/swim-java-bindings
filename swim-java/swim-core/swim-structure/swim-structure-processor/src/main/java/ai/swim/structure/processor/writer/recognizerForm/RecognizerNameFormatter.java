package ai.swim.structure.processor.writer.recognizerForm;


import ai.swim.structure.processor.writer.NameFormatter;
import javax.lang.model.element.PackageElement;

public class RecognizerNameFormatter extends NameFormatter {
  public RecognizerNameFormatter(String name, PackageElement packageElement) {
    super(name, packageElement);
  }


  /**
   * Returns a string with "Builder" suffixed.
   */
  public String builderClassName() {
    return String.format("%sBuilder", this.name);
  }

  /**
   * Returns a string with "HeaderBuilder" suffixed.
   */
  public String headerBuilderClassName() {
    return String.format("%sHeaderBuilder", this.name);
  }

  /**
   * Returns a static class canonical representation of a header builder. E.g, "ai.swim.Prop.HeaderBuilder".
   */
  public String headerBuilderCanonicalName() {
    return String.format(
        "%s.%s.%s",
        this.packageElement.getQualifiedName(),
        this.recognizerClassName(),
        this.headerBuilderClassName());
  }

  /**
   * Returns a string with "Header" suffixed.
   */
  public String headerClassName() {
    return String.format("%sHeader", this.name);
  }

  /**
   * Returns a static class canonical representation of a header. E.g, "ai.swim.Prop.Header".
   */
  public String headerCanonicalName() {
    return String.format(
        "%s.%s.%sHeader",
        this.packageElement.getQualifiedName(),
        this.recognizerClassName(),
        this.name);
  }

  /**
   * Returns a field name for a header builder.
   */
  public String headerBuilderFieldName() {
    return "headerBuilder";
  }

  /**
   * Returns a string with "Builder" suffixed to 'fieldName'.
   */
  public String fieldBuilderName(String fieldName) {
    return String.format("%sBuilder", fieldName);
  }

  /**
   * Returns "ai.swim.structure.recognizer.structural.delegate.HeaderRecognizer.headerBuilder".
   */
  public String headerBuilderMethod() {
    return "ai.swim.structure.recognizer.structural.delegate.HeaderRecognizer.headerBuilder";
  }

  /**
   * Returns a string with "Recognizer" suffixed.
   */
  public String recognizerClassName() {
    return recognizerClassName(this.name);
  }

  /**
   * Returns a string with "Recognizer" suffixed.
   */
  public String recognizerClassName(CharSequence name) {
    return String.format("%sRecognizer", name);
  }

  /**
   * Returns a string with "ConcreteRecognizer" suffixed.
   */
  public String concreteRecognizerClassName() {
    return String.format("%sConcreteRecognizer", this.name);
  }

  /**
   * Returns a string with "Spec" suffixed.
   */
  public String enumSpec() {
    return String.format("%sSpec", this.name);
  }
}
