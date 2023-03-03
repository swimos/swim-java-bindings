package ai.swim.recon.event.number;

import ai.swim.recon.event.ReadEventVisitor;

public class ReadLongValue extends ReadNumberValue<Long> {
  public ReadLongValue(Long value) {
    super(value);
  }

  @Override
  public boolean isReadLong() {
    return true;
  }

  @Override
  public <O> O visit(ReadEventVisitor<O> visitor) {
    return visitor.visitLong(value);
  }
}
