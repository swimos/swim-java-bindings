package ai.swim.structure.reflect.clazz;

public abstract class ValueProxy {
  private final String fieldName;

  protected ValueProxy(String fieldName) {
    this.fieldName = fieldName;
  }

  public String getFieldName() {
    return fieldName;
  }
}
