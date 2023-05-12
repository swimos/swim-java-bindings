package ai.swim.structure.processor.model.mapping;

public enum CoreTypeKind {
  Character(java.lang.Character.class),
  Byte(java.lang.Byte.class),
  Short(java.lang.Short.class),
  Integer(java.lang.Integer.class),
  Long(java.lang.Long.class),
  Float(java.lang.Float.class),
  Double(java.lang.Double.class),
  Boolean(java.lang.Boolean.class),
  String(java.lang.String.class),
  Number(java.lang.Number.class),
  BigInteger(java.math.BigInteger.class),
  BigDecimal(java.math.BigDecimal.class);

  private final Class<?> clazz;

  CoreTypeKind(Class<?> clazz) {
    this.clazz = clazz;
  }

  public String getPackageName() {
    return clazz.getPackageName();
  }
}
