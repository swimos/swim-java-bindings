package ai.swim.recon.event.number;

import ai.swim.recon.event.ReadEvent;
import ai.swim.recon.event.ReadEventVisitor;

import java.math.BigDecimal;
import java.util.Objects;

public class ReadBigDecimalValue extends ReadEvent {
  private final BigDecimal value;

  public ReadBigDecimalValue(BigDecimal value) {
    this.value = value;
  }

  @Override
  public boolean isReadBigDecimal() {
    return true;
  }

  @Override
  public <O> O visit(ReadEventVisitor<O> visitor) {
    return visitor.visitBigDecimal(value);
  }

  @Override
  public boolean isPrimitive() {
    return true;
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
    ReadBigDecimalValue that = (ReadBigDecimalValue) o;
    return Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public String toString() {
    return "ReadBigDecimalValue{" +
        "value=" + value +
        '}';
  }

  public BigDecimal getValue() {
    return value;
  }
}
