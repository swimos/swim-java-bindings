package ai.swim.recon.event.number;

import ai.swim.recon.event.ReadEventVisitor;

public class ReadFloatValue extends ReadNumberValue<Float> {
  public ReadFloatValue(Float value) {
    super(value);
  }

  @Override
  public boolean isReadFloat() {
    return true;
  }

  @Override
  public <O> O visit(ReadEventVisitor<O> visitor) {
    return visitor.visitFloat(value);
  }
}
