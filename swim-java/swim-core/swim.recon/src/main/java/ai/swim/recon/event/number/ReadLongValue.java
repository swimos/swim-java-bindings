package ai.swim.recon.event.number;

import ai.swim.recon.event.ReadEvent;
import ai.swim.recon.event.ReadEventVisitor;

import java.util.Objects;

public class ReadLongValue extends ReadEvent {
  private final Long value;

  public ReadLongValue(Long value) {
    this.value = value;
  }

  @Override
  public boolean isReadLong() {
    return true;
  }

  @Override
  public <O> O visit(ReadEventVisitor<O> visitor) {
    return visitor.visitLong(value);
  }

  @Override
  public String toString() {
    return "ReadLongValue{" +
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
    ReadLongValue that = (ReadLongValue) o;
    return Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  public Long getValue() {
    return value;
  }

  @Override
  public boolean isPrimitive() {
    return true;
  }
}
