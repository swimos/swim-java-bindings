package ai.swim.recon.event.number;

import ai.swim.recon.event.ReadEventVisitor;

public class ReadIntValue extends ReadNumberValue<Integer> {
  public ReadIntValue(Integer value) {
    super(value);
  }

  @Override
  public boolean isReadInt() {
    return true;
  }

  @Override
  public <O> O visit(ReadEventVisitor<O> visitor) {
    return visitor.visitInt(value);
  }
}
