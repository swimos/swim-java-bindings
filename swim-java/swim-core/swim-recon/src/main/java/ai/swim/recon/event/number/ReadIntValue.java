package ai.swim.recon.event.number;

import ai.swim.recon.event.ReadEvent;
import ai.swim.recon.event.ReadEventVisitor;
import java.util.Objects;

public class ReadIntValue extends ReadEvent {
  private final Integer value;

  public ReadIntValue(Integer value) {
    this.value = value;
  }

  @Override
  public boolean isReadInt() {
    return true;
  }

  @Override
  public <O> O visit(ReadEventVisitor<O> visitor) {
    return visitor.visitInt(value);
  }

  public Integer getValue() {
    return value;
  }

  @Override
  public boolean isPrimitive() {
    return true;
  }

  @Override
  public String toString() {
    return "ReadIntValue{" +
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
    ReadIntValue that = (ReadIntValue) o;
    return Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }
}
