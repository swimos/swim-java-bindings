package ai.swim.structure.recognizer.structural.delegate;

import ai.swim.recon.event.ReadEvent;
import ai.swim.structure.FieldRecognizingBuilder;
import ai.swim.structure.RecognizingBuilder;
import ai.swim.structure.annotations.AutoloadedRecognizer;
import ai.swim.structure.recognizer.Recognizer;
import ai.swim.structure.recognizer.proxy.RecognizerProxy;
import ai.swim.structure.recognizer.structural.tag.FixedTagSpec;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

import static ai.swim.structure.RecognizerTestUtil.runTest;
import static ai.swim.structure.recognizer.structural.delegate.HeaderRecognizer.headerBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DelegateClassRecognizerTest {

  public static class PropHeader {
    int a;
    String b;

    public PropHeader(int a, String b) {
      this.a = a;
      this.b = b;
    }
  }

  public static class PropHeaderBuilder implements RecognizingBuilder<PropHeader> {
    private final RecognizingBuilder<Integer> aBuilder = new FieldRecognizingBuilder<>(Integer.class);
    private final RecognizingBuilder<String> bBuilder = new FieldRecognizingBuilder<>(String.class);

    @Override
    public boolean feedIndexed(int index, ReadEvent event) {
      if (index == 0) {
        return this.aBuilder.feed(event);
      } else if (index == 1) {
        return this.bBuilder.feed(event);
      } else {
        throw new RuntimeException();
      }
    }

    @Override
    public PropHeader bind() {
      return new PropHeader(this.aBuilder.bind(), this.bBuilder.bind());
    }

    @Override
    public RecognizingBuilder<PropHeader> reset() {
      return new PropHeaderBuilder();
    }
  }

  public static class Prop {
    private int a; // header
    private String b; // header
    private String c; // body

    public Prop() {

    }

    public Prop(int a, String b, String c) {
      this.a = a;
      this.b = b;
      this.c = c;
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

    @Override
    public String toString() {
      return "Prop{" +
          "a=" + a +
          ", b='" + b + '\'' +
          ", c='" + c + '\'' +
          '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof Prop)) return false;
      Prop prop = (Prop) o;
      return a == prop.a && Objects.equals(b, prop.b) && Objects.equals(c, prop.c);
    }

    @Override
    public int hashCode() {
      return Objects.hash(a, b, c);
    }
  }

  public static class PropBuilder implements RecognizingBuilder<Prop> {
    private RecognizingBuilder<PropHeader> headerBuilder = headerBuilder(
        false,
        PropHeaderBuilder::new,
        1,
        (key) -> {
          if (key.isHeaderSlot()) {
            HeaderFieldKey.HeaderSlotKey headerSlotKey = (HeaderFieldKey.HeaderSlotKey) key;
            if ("a".equals(headerSlotKey.getName())) {
              return 0;
            } else if ("b".equals(headerSlotKey.getName())) {
              return 1;
            }
          }
          return null;
        }
    );
    private RecognizingBuilder<String> bBuilder = new FieldRecognizingBuilder<>(RecognizerProxy.getInstance().lookup(String.class).asBodyRecognizer());

    @Override
    public boolean feedIndexed(int index, ReadEvent event) {
      switch (index) {
        case 0:
          return this.headerBuilder.feed(event);
        case 1:
          return this.bBuilder.feed(event);
        default:
          throw new RuntimeException("Unknown idx: " + index);
      }
    }

    @Override
    public Prop bind() {
      PropHeader header = this.headerBuilder.bind();

      return new Prop(header.a, header.b, this.bBuilder.bind());
    }

    @Override
    public RecognizingBuilder<Prop> reset() {
      this.headerBuilder = this.headerBuilder.reset();
      this.bBuilder = this.bBuilder.reset();

      return this;
    }
  }

  @AutoloadedRecognizer(Prop.class)
  public static class PropRecognizer extends Recognizer<Prop> {
    private Recognizer<Prop> delegate;

    public PropRecognizer() {
      this.delegate = new DelegateClassRecognizer<>(
          new FixedTagSpec(Prop.class.getSimpleName()),
          new PropBuilder(),
          3,
          (key) -> {
            if (key.isHeader()) {
              return 0;
            }
            if (key.isFirstItem()) {
              return 1;
            }

            return null;
          }
      );
    }

    @Override
    public Recognizer<Prop> feedEvent(ReadEvent event) {
      this.delegate = this.delegate.feedEvent(event);
      return this;
    }

    @Override
    public Recognizer<Prop> reset() {
      return new PropRecognizer();
    }

    @Override
    public boolean isError() {
      return this.delegate.isError();
    }

    @Override
    public RuntimeException trap() {
      return this.delegate.trap();
    }

    @Override
    public boolean isDone() {
      return this.delegate.isDone();
    }

    @Override
    public Prop bind() {
      return this.delegate.bind();
    }
  }

  @Test
  void testDelegateRecognizer() {
    Recognizer<Prop> recognizer = new PropRecognizer();
    List<ReadEvent> events = List.of(
        ReadEvent.startAttribute("Prop"),
        ReadEvent.text("a"),
        ReadEvent.slot(),
        ReadEvent.number(1),
        ReadEvent.text("b"),
        ReadEvent.slot(),
        ReadEvent.text("b"),
        ReadEvent.endAttribute(),
        ReadEvent.startBody(),
        ReadEvent.text("string"),
        ReadEvent.endRecord()
    );

    Prop obj = runTest(recognizer, events);
    assertEquals(obj, new Prop(1, "b", "string"));
  }

}