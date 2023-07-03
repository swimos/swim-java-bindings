package ai.swim.structure.recognizer.structural.tag;

public class FixedTagSpec extends TagSpec {

  private final String tag;

  public FixedTagSpec(String tag) {
    this.tag = tag;
  }

  @Override
  public boolean isFixed() {
    return true;
  }

  public String getTag() {
    return tag;
  }

}
