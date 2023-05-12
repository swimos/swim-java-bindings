package ai.swim.recon.event.number;

import ai.swim.recon.event.ReadEvent;
import ai.swim.recon.event.ReadEventVisitor;

import java.util.Objects;

public class ReadDoubleValue extends ReadEvent {
  private final Double value;

  public ReadDoubleValue(Double value) {
    this.value = value;
  }

  @Override
  public boolean isReadDouble() {
    return true;
  }

  @Override
  public boolean isPrimitive() {
    return true;
  }

  @Override
  public <O> O visit(ReadEventVisitor<O> visitor) {
    return visitor.visitDouble(value);
  }

  public Double getValue() {
    return value;
  }

  @Override
  public String toString() {
    return "ReadDoubleValue{" +
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
    ReadDoubleValue that = (ReadDoubleValue) o;
    return Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }
}
