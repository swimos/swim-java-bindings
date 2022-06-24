package ai.swim.structure.recognizer.structural;

import ai.swim.structure.annotations.AutoForm;
import ai.swim.structure.recognizer.Recognizer;
import ai.swim.structure.recognizer.proxy.RecognizerProxy;
import org.junit.jupiter.api.Test;

public class AutoStructuralGenericTest {

  @AutoForm
  public static class SimpleGeneric<G extends Number, A extends Number> {
    public G generic;
    public Typed<A> a;
    public boolean c;
  }

  public static class Typed<A extends Number> {
    public A a;
  }

  @Test
  void testSimpleClass() {

  }

}
