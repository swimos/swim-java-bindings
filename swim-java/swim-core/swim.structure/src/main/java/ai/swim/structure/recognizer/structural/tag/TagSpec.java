package ai.swim.structure.recognizer.structural.tag;

public abstract class TagSpec {

  public boolean isFixed() {
    return false;
  }

  public boolean isField() {
    return false;
  }

  public boolean isFixedSet() {
    return false;
  }

}
