package ai.swim.recon.event.number;

import ai.swim.recon.event.ReadEventVisitor;

import java.math.BigDecimal;

public class ReadBigDecimalValue extends ReadNumberValue<BigDecimal> {
  public ReadBigDecimalValue(BigDecimal value) {
    super(value);
  }

  @Override
  public boolean isReadBigDecimal() {
    return true;
  }

  @Override
  public <O> O visit(ReadEventVisitor<O> visitor) {
    return visitor.visitBigDecimal(value);
  }
}
