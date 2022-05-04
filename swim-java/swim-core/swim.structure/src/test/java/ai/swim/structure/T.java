package ai.swim.structure;

import ai.swim.recon.event.ReadEvent;
import ai.swim.structure.annotations.AutoForm;
import ai.swim.structure.recognizer.Recognizer;
import org.junit.jupiter.api.Test;

import java.util.List;

public class T {

  @AutoForm
  public static class PropParent {
    public int parentA;

    public void setParentA(int parentA) {
      this.parentA = parentA;
    }
  }

  @AutoForm
  public static class Prop extends PropParent {
    public static class Temp {

    }

    private float a;
    private List<Integer> b;
    public int c;

    @AutoForm.Optional
    private Integer skipped;

    @AutoForm.Setter("a")
    public void setFieldA(float a) {
      this.a = a;
    }

    public void setB(List<Integer> b) {
      this.b = b;
    }
  }

  @Test
  public void t() {

  }
}
