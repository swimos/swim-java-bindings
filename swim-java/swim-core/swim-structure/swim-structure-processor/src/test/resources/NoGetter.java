import ai.swim.structure.annotations.AutoForm;

@AutoForm
public class NoGetter {
  private int a;

  public void setA(int a) {
    this.a = a;
  }
}