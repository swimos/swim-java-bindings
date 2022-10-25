import ai.swim.structure.annotations.AutoForm;

@AutoForm
public class BadGetter {
  private int a;

  public float getA() {
    return 1.1f;
  }

  public void setA(int a) {
    this.a = a;
  }
}