package ai.swim.recon.event.number;

import ai.swim.recon.event.ReadEvent;
import ai.swim.recon.event.ReadEventVisitor;

import java.util.Objects;

public class ReadFloatValue extends ReadEvent {
  private final Float value;

  public ReadFloatValue(Float value) {
    this.value = value;
  }

  @Override
  public boolean isReadFloat() {
    return true;
  }

  @Override
  public <O> O visit(ReadEventVisitor<O> visitor) {
    return visitor.visitFloat(value);
  }

  public Float getValue() {
    return value;
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
    ReadFloatValue that = (ReadFloatValue) o;
    return Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public String toString() {
    return "ReadFloatValue{" +
        "value=" + value +
        '}';
  }
}
