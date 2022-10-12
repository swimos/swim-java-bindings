package ai.swim.recon.event.number;

public class ReadFloatValue extends ReadNumberValue<Float> {
  public ReadFloatValue(Float value) {
    super(value);
  }

  @Override
  public boolean isReadFloat() {
    return true;
  }
}
