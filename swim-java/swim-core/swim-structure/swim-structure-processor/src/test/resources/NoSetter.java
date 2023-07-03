import ai.swim.structure.annotations.AutoForm;

@AutoForm
public class NoSetter {
  private int a;

  public int getA() {
    return a;
  }
}