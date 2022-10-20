package ai.swim.recon.event.number;

import ai.swim.recon.event.ReadEventVisitor;

import java.math.BigInteger;

public class ReadBigIntValue extends ReadNumberValue<BigInteger> {
  public ReadBigIntValue(BigInteger value) {
    super(value);
  }

  @Override
  public boolean isReadBigInt() {
    return true;
  }

  @Override
  public <O> O visit(ReadEventVisitor<O> visitor) {
    return visitor.visitBigInt(value);
  }
}
