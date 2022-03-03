package ai.swim.structure.form.recognizer.structural.key;

public class ItemFieldKey extends LabelledFieldKey {
  private final String name;

  public ItemFieldKey(String name) {
    this.name = name;
  }

  @Override
  public boolean isItem() {
    return true;
  }

  public String getName() {
    return name;
  }
}
