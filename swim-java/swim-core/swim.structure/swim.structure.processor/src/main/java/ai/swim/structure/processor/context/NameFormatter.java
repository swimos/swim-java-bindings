package ai.swim.structure.processor.context;

public class NameFormatter {
  private final String name;

  public NameFormatter(String name) {
    this.name = name;
  }

  public String builderClassName() {
    return String.format("%sBuilder", this.name);
  }

  public String headerBuilderClassName() {
    return String.format("%sHeaderBuilder", this.name);
  }

  public String headerBuilderFieldName() {
    return "headerBuilder";
  }

  public String fieldBuilderName(String fieldName) {
    return String.format("%sBuilder", fieldName);
  }

}
