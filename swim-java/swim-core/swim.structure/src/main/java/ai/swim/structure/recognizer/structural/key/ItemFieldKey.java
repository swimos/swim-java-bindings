package ai.swim.structure.recognizer.structural.key;

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

  @Override
  public String toString() {
    return "ItemFieldKey{" +
        "name='" + name + '\'' +
        '}';
  }
}
