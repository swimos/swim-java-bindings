package ai.swim.codec.parsers.number;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

/**
 * A typed, non-converting, number wrapper.
 */
public abstract class TypedNumber {

  private final Object value;

  protected TypedNumber(Object value) {
    this.value = value;
  }

  public boolean isInt() {
    return false;
  }

  public int intValue() {
    if (!isInt()) {
      throw new IllegalStateException("Not an int value");
    }
    return (int) value;
  }

  public boolean isLong() {
    return false;
  }

  public long longValue() {
    if (!isLong()) {
      throw new IllegalStateException("Not a long value");
    }
    return (long) value;
  }

  public boolean isFloat() {
    return false;
  }

  public float floatValue() {
    if (!isFloat()) {
      throw new IllegalStateException("Not a float value");
    }
    return (float) value;
  }

  public boolean isDouble() {
    return false;
  }

  public double doubleValue() {
    if (!isDouble()) {
      throw new IllegalStateException("Not a double value");
    }
    return (double) value;
  }

  public boolean isBigInt() {
    return false;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof TypedNumber)) return false;
    TypedNumber that = (TypedNumber) o;
    return Objects.equals(value, that.value);
  }

  @Override
  public String toString() {
    return "TypedNumber{" +
        "value=" + value +
        '}';
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  public BigInteger bigIntValue() {
    if (!isBigInt()) {
      throw new IllegalStateException("Not a big int value");
    }
    return (BigInteger) value;
  }

  public boolean isBigDecimal() {
    return false;
  }

  public BigDecimal bigDecimalValue() {
    if (!isBigDecimal()) {
      throw new IllegalStateException("Not a big decimal value");
    }
    return (BigDecimal) value;
  }

  public static TypedNumber intNumber(int value) {
    return new TypedNumber(value) {
      @Override
      public boolean isInt() {
        return true;
      }
    };
  }

  public static TypedNumber longNumber(long value) {
    return new TypedNumber(value) {
      @Override
      public boolean isLong() {
        return true;
      }
    };
  }

  public static TypedNumber floatNumber(float value) {
    return new TypedNumber(value) {
      @Override
      public boolean isFloat() {
        return true;
      }
    };
  }

  public static TypedNumber doubleNumber(double value) {
    return new TypedNumber(value) {
      @Override
      public boolean isDouble() {
        return true;
      }
    };
  }

  public static TypedNumber bigIntNumber(BigInteger value) {
    return new TypedNumber(value) {
      @Override
      public boolean isBigInt() {
        return true;
      }
    };
  }

  public static TypedNumber bigDecimalNumber(BigDecimal value) {
    return new TypedNumber(value) {
      @Override
      public boolean isBigDecimal() {
        return true;
      }
    };
  }

}
