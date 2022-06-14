package ai.swim.recon.event.number;

import java.math.BigInteger;

public class ReadBigIntValue extends ReadNumberValue<BigInteger> {
  public ReadBigIntValue(BigInteger value) {
    super(value);
  }

  @Override
  public boolean isReadBigInt() {
    return true;
  }
}
