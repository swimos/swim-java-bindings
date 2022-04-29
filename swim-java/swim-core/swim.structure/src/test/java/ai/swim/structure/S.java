package ai.swim.structure;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.List;

public class S {

  abstract static class ISchema<T> {
    abstract boolean isValid(T t);
  }

  @Target(ElementType.METHOD)
  @interface Schema {
    String value() default "";

    @Target(ElementType.FIELD)
    @interface Range {
      interface Ranged {
        boolean inBound(int min, int max);
      }

      final class Default implements Ranged {
        private Default() {
          throw new AssertionError();
        }

        @Override
        public boolean inBound(int min, int max) {
          throw new AssertionError();
        }
      }

      int min() default 0;

      int max() default 0;

      Class<?> with() default Default.class;
    }

    @Target(ElementType.FIELD)
    @interface Len {
      int value();
    }
  }

  static class ValueSchema {

  }

  private final static class Test {
    @Schema.Range(min = 0, max = 100)
    private String a;

    @Schema.Len(5)
    private List<String> list;

    private String val;

    @Schema("val")
    public ValueSchema valSchema() {
      return null;
    }
  }

  static class A {
  }

  static class ASchema extends ISchema<A> {
    @Override
    public boolean isValid(A a) {
      return false;
    }
  }

  static class ValidSchema extends ISchema<Object> {
    @Override
    boolean isValid(Object o) {
      return true;
    }
  }

  static class InvalidSchema extends ISchema<Object> {
    @Override
    boolean isValid(Object o) {
      return false;
    }
  }

  void view() {
    A a = new A();
    ISchema<A> schema = new ASchema();

  }
}
