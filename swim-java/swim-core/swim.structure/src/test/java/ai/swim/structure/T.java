package ai.swim.structure;

import ai.swim.structure.annotations.AutoForm;
import org.junit.jupiter.api.Test;

import java.util.List;

public class T {

  @AutoForm
  public static class Prop<E extends Number> {
    public static class Temp {

    }

    private float a;
    private List<Integer> b;

    public void setB(List<Integer> b) {
      this.b = b;
    }

    @AutoForm.Setter("a")
    public void setField(float a) {
      this.a = a;
    }
  }


  @Test
  public void t() {

  }
}
