package ai.swim.structure.reflect;

public class CoreTypeLayout extends TypeLayout {

  private final static Class<?> BOOLEAN_CLASS = Boolean.TYPE;
  private final static Class<?> INTEGER_CLASS = Integer.TYPE;
  private final static Class<?> FLOAT_CLASS = Float.TYPE;
  private final static Class<?> LONG_CLASS = Long.TYPE;
  private final static Class<?> CHARACTER_CLASS = Character.TYPE;
  private final static Class<?> BYTE_CLASS = Byte.TYPE;
  private final static Class<?> SHORT_CLASS = Short.TYPE;
  private final static Class<?> STRING_CLASS = String.class;
  private final static Class<?> OBJECT_CLASS = Object.class;
  private final static Class<?> BOXED_INTEGER_CLASS = Integer.class;
  private final static Class<?> BOXED_FLOAT_CLASS = Float.class;
  private final static Class<?> BOXED_LONG_CLASS = Long.class;
  private final static Class<?> BOXED_BYTE_CLASS = Byte.class;
  private final static Class<?> BOXED_CHARACTER_CLASS = Character.class;
  private final static Class<?> BOXED_SHORT_CLASS = Short.class;
  private final static Class<?> BOXED_DOUBLE_CLASS = Double.class;
  private final static Class<?> BOXED_BOOLEAN_CLASS = Boolean.class;

  private final static TypeLayout TYPE_BOOLEAN = new CoreTypeLayout(BOOLEAN_CLASS);
  private final static TypeLayout TYPE_INTEGER = new CoreTypeLayout(INTEGER_CLASS);
  private final static TypeLayout TYPE_FLOAT = new CoreTypeLayout(FLOAT_CLASS);
  private final static TypeLayout TYPE_LONG = new CoreTypeLayout(LONG_CLASS);
  private final static TypeLayout TYPE_CHARACTER = new CoreTypeLayout(CHARACTER_CLASS);
  private final static TypeLayout TYPE_BYTE = new CoreTypeLayout(BYTE_CLASS);
  private final static TypeLayout TYPE_SHORT = new CoreTypeLayout(SHORT_CLASS);
  private final static TypeLayout TYPE_STRING = new CoreTypeLayout(STRING_CLASS);
  private final static TypeLayout TYPE_OBJECT = new CoreTypeLayout(OBJECT_CLASS);
  private final static TypeLayout TYPE_BOXED_INTEGER = new CoreTypeLayout(BOXED_INTEGER_CLASS);
  private final static TypeLayout TYPE_BOXED_FLOAT = new CoreTypeLayout(BOXED_FLOAT_CLASS);
  private final static TypeLayout TYPE_BOXED_LONG = new CoreTypeLayout(BOXED_LONG_CLASS);
  private final static TypeLayout TYPE_BOXED_BYTE = new CoreTypeLayout(BOXED_BYTE_CLASS);
  private final static TypeLayout TYPE_BOXED_CHARACTER = new CoreTypeLayout(BOXED_CHARACTER_CLASS);
  private final static TypeLayout TYPE_BOXED_SHORT = new CoreTypeLayout(BOXED_SHORT_CLASS);
  private final static TypeLayout TYPE_BOXED_DOUBLE = new CoreTypeLayout(BOXED_DOUBLE_CLASS);
  private final static TypeLayout TYPE_BOXED_BOOLEAN = new CoreTypeLayout(BOXED_BOOLEAN_CLASS);

  public CoreTypeLayout(Class<?> binding) {
    super(binding);
  }

  public static TypeLayout of(Class<?> clazz) {
    if (CoreTypeLayout.isCoreType(clazz)) {
      if (clazz == BOOLEAN_CLASS) return TYPE_BOOLEAN;
      if (clazz == INTEGER_CLASS) return TYPE_INTEGER;
      if (clazz == FLOAT_CLASS) return TYPE_FLOAT;
      if (clazz == LONG_CLASS) return TYPE_LONG;
      if (clazz == CHARACTER_CLASS) return TYPE_CHARACTER;
      if (clazz == BYTE_CLASS) return TYPE_BYTE;
      if (clazz == SHORT_CLASS) return TYPE_SHORT;
      if (clazz == STRING_CLASS) return TYPE_STRING;
      if (clazz == OBJECT_CLASS) return TYPE_OBJECT;
      if (clazz == BOXED_INTEGER_CLASS) return TYPE_BOXED_INTEGER;
      if (clazz == BOXED_FLOAT_CLASS) return TYPE_BOXED_FLOAT;
      if (clazz == BOXED_LONG_CLASS) return TYPE_BOXED_LONG;
      if (clazz == BOXED_CHARACTER_CLASS) return TYPE_BOXED_CHARACTER;
      if (clazz == BOXED_BYTE_CLASS) return TYPE_BOXED_BYTE;
      if (clazz == BOXED_SHORT_CLASS) return TYPE_BOXED_SHORT;
      if (clazz == BOXED_DOUBLE_CLASS) return TYPE_BOXED_DOUBLE;
      if (clazz == BOXED_BOOLEAN_CLASS) return TYPE_BOXED_BOOLEAN;
    }

    throw new IllegalArgumentException("Attempted to create a primitive type layout from: " + clazz.getCanonicalName());
  }

  /**
   *  Returns whether the class represents a core data type; primitive, boxed primitive, string or object.
   */
  public static boolean isCoreType(Class<?> clazz) {
    return clazz.isPrimitive()
        || clazz == OBJECT_CLASS
        || clazz == STRING_CLASS
        || clazz == BOXED_INTEGER_CLASS
        || clazz == BOXED_FLOAT_CLASS
        || clazz == BOXED_LONG_CLASS
        || clazz == BOXED_CHARACTER_CLASS
        || clazz == BOXED_BYTE_CLASS
        || clazz == BOXED_SHORT_CLASS
        || clazz == BOXED_DOUBLE_CLASS
        || clazz == BOXED_BOOLEAN_CLASS;
  }

  @Override
  public boolean equals(Object obj) {
    return super.equals(obj);
  }

  @Override
  public String toString() {
    return super.toString();
  }
}
