import ai.swim.structure.annotations.AutoForm;

@AutoForm
public class BadSetter {
  private int a;

  public int getA() {
    return a;
  }

  public void setA(float a) {

  }
}