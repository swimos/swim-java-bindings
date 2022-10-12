package ai.swim.recon.event.number;

public class ReadIntValue extends ReadNumberValue<Integer> {
  public ReadIntValue(Integer value) {
    super(value);
  }

  @Override
  public boolean isReadInt() {
    return true;
  }
}
