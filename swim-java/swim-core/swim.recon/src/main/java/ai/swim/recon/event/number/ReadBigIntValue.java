package ai.swim.recon.event.number;

import ai.swim.recon.event.ReadEvent;
import ai.swim.recon.event.ReadEventVisitor;

import java.math.BigInteger;
import java.util.Objects;

public class ReadBigIntValue extends ReadEvent {
  private final BigInteger value;

  public ReadBigIntValue(BigInteger value) {
    this.value = value;
  }

  @Override
  public boolean isReadBigInt() {
    return true;
  }

  @Override
  public <O> O visit(ReadEventVisitor<O> visitor) {
    return visitor.visitBigInt(value);
  }

  public BigInteger getValue() {
    return value;
  }

  @Override
  public boolean isPrimitive() {
    return true;
  }

  @Override
  public String toString() {
    return "ReadBigIntValue{" +
      "value=" + value +
      '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    ReadBigIntValue that = (ReadBigIntValue) o;
    return Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }
}
