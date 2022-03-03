package ai.swim.structure.form.recognizer.structural.key;

public class AttrFieldKey extends LabelledFieldKey {
  private final String key;

  public AttrFieldKey(String key) {
    this.key = key;
  }

  @Override
  public boolean isAttribute() {
    return true;
  }

  public String getKey() {
    return key;
  }
}
