package ai.swim.structure.recognizer.structural.key;

public abstract class LabelledFieldKey {

  public boolean isTag() {
    return false;
  }

  public boolean isHeader() {
    return false;
  }

  public boolean isAttribute() {
    return false;
  }

  public boolean isItem() {
    return false;
  }

}
