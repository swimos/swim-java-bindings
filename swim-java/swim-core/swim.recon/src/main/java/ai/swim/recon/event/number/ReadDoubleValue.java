package ai.swim.recon.event.number;

public class ReadDoubleValue extends ReadNumberValue<Double> {
  public ReadDoubleValue(Double value) {
    super(value);
  }

  @Override
  public boolean isReadDouble() {
    return true;
  }
}
