package ai.swim.structure.recognizer.structural.delegate;

import ai.swim.structure.annotations.AutoForm;
import ai.swim.structure.annotations.FieldKind;

public class AutoDelegateTest {

  @AutoForm
  public static class Prop {
    @AutoForm.Kind(FieldKind.Header)
    private int a;
    @AutoForm.Kind(FieldKind.Header)
    private String b;
    @AutoForm.Kind(FieldKind.Body)
    private String c;

    public Prop() {

    }

    public void setA(int a) {
      this.a = a;
    }

    public void setB(String b) {
      this.b = b;
    }

    public void setC(String c) {
      this.c = c;
    }
  }

}
