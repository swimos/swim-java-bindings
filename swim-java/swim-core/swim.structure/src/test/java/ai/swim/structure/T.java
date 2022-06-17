package ai.swim.structure;

import ai.swim.recon.event.ReadEvent;
import ai.swim.structure.annotations.AutoForm;
import ai.swim.structure.recognizer.Recognizer;
import org.junit.jupiter.api.Test;

import java.util.List;

public class T {

//  @AutoForm
//  public static class PropParent {
//    public int parentA;
//
//    public void setParentA(int parentA) {
//      this.parentA = parentA;
//    }
//  }

  @AutoForm
  public static class Prop {
    private int a;
    private List<Integer> b;

    public void setA(int a) {
      this.a = a;
    }

    public void setB(List<Integer> b) {
      this.b = b;
    }

    @Override
    public String toString() {
      return "Prop{" +
          "a=" + a +
          ", b=" + b +
          '}';
    }
  }

  @Test
  public void t() {
    Recognizer<Prop> recognizer = new PropRecognizer();
    List<ReadEvent> readEvents = List.of(
        ReadEvent.startAttribute("Prop"),
        ReadEvent.endAttribute(),
        ReadEvent.startBody(),
        ReadEvent.text("a"),
        ReadEvent.slot(),
        ReadEvent.number(1),
        ReadEvent.text("b"),
        ReadEvent.slot(),
        ReadEvent.startBody(),
        ReadEvent.number(2),
        ReadEvent.number(3),
        ReadEvent.number(4),
        ReadEvent.endRecord(),
        ReadEvent.endRecord()
    );

    for (ReadEvent readEvent : readEvents) {
      recognizer = recognizer.feedEvent(readEvent);
    }

    System.out.println(recognizer.bind());
  }
}
