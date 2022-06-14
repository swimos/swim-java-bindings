package ai.swim.recon.event.number;

public class ReadLongValue extends ReadNumberValue<Long> {
  public ReadLongValue(Long value) {
    super(value);
  }

  @Override
  public boolean isReadLong() {
    return true;
  }
}
