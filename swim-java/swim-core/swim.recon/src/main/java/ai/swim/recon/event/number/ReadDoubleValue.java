package ai.swim.recon.event.number;

import ai.swim.recon.event.ReadEventVisitor;

public class ReadDoubleValue extends ReadNumberValue<Double> {
  public ReadDoubleValue(Double value) {
    super(value);
  }

  @Override
  public boolean isReadDouble() {
    return true;
  }

  @Override
  public <O> O visit(ReadEventVisitor<O> visitor) {
    return visitor.visitDouble(value);
  }
}
