package ai.swim.structure.processor.model;

public class CoreTypeSpec<T> {
  private final Class<T> clazz;
  private final CoreTypeModel.Kind kind;
  private final T defaultValue;

  public CoreTypeSpec(Class<T> clazz, CoreTypeModel.Kind kind, T defaultValue) {
    this.clazz = clazz;
    this.kind = kind;
    this.defaultValue = defaultValue;
  }

  public Class<T> getClazz() {
    return clazz;
  }

  public CoreTypeModel.Kind getKind() {
    return kind;
  }

  public T getDefaultValue() {
    return defaultValue;
  }
}