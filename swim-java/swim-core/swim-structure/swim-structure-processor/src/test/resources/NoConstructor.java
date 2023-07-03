import ai.swim.structure.annotations.AutoForm;

@AutoForm
public class NoConstructor {
  private int a;

  public NoConstructor(int a) {
    this.a = a;
  }

  public int getA() {
    return a;
  }

  public void setA(int a) {
    this.a = a;
  }
}