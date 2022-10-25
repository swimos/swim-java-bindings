import ai.swim.structure.annotations.AutoForm;

@AutoForm
public class BadGetter2 {
  private int a;

  public int getA(int a) {
    return 1.1f;
  }

  public void setA(int a) {
    this.a = a;
  }
}