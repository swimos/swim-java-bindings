package ai.swim.recon.event.number;

import java.math.BigDecimal;

public class ReadBigDecimalValue extends ReadNumberValue<BigDecimal> {
  public ReadBigDecimalValue(BigDecimal value) {
    super(value);
  }

  @Override
  public boolean isReadBigDecimal() {
    return true;
  }
}
