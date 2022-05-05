package ai.swim.structure;

import ai.swim.recon.event.ReadEvent;
import ai.swim.structure.annotations.AutoloadedRecognizer;
import ai.swim.structure.recognizer.Recognizer;
import ai.swim.structure.recognizer.structural.ClassRecognizerInit;
import ai.swim.structure.recognizer.structural.key.ItemFieldKey;
import ai.swim.structure.recognizer.structural.tag.FixedTagSpec;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class ClassRecognizerTest {

  @Test
  void recognizeClass() throws Exception {
    Recognizer<InnerPropClass> recognizer = new InnerClassRecognizer();

    List<ReadEvent> readEvents = new ArrayList<>();
    readEvents.add(ReadEvent.startAttribute("InnerPropClass"));
    readEvents.add(ReadEvent.extant());
    readEvents.add(ReadEvent.endAttribute());
    readEvents.add(ReadEvent.startBody());
    readEvents.add(ReadEvent.text("a"));
    readEvents.add(ReadEvent.slot());
    readEvents.add(ReadEvent.number(1));
    readEvents.add(ReadEvent.text("b"));
    readEvents.add(ReadEvent.slot());
    readEvents.add(ReadEvent.number(2));
    readEvents.add(ReadEvent.endRecord());

    for (ReadEvent event : readEvents) {
      recognizer = recognizer.feedEvent(event);
      if (recognizer.isError()) {
        throw recognizer.trap();
      }
    }

    if (!recognizer.isDone()) {
      throw new RuntimeException("Recognizer did not complete");
    }
    if (recognizer.isError()) {
      throw recognizer.trap();
    }

    System.out.println(recognizer.bind());
  }

  @Test
  void recognizeNestedClass() throws Exception {
    OuterClassRecognizer recognizer = new OuterClassRecognizer();

    List<ReadEvent> readEvents = new ArrayList<>();
    readEvents.add(ReadEvent.startAttribute("OuterPropClass"));
    readEvents.add(ReadEvent.extant());
    readEvents.add(ReadEvent.endAttribute());
    readEvents.add(ReadEvent.startBody());
    readEvents.add(ReadEvent.text("c"));
    readEvents.add(ReadEvent.slot());
    readEvents.add(ReadEvent.text("string"));
    readEvents.add(ReadEvent.text("d"));
    readEvents.add(ReadEvent.slot());

    readEvents.add(ReadEvent.startAttribute("InnerPropClass"));
    readEvents.add(ReadEvent.extant());
    readEvents.add(ReadEvent.endAttribute());
    readEvents.add(ReadEvent.startBody());
    readEvents.add(ReadEvent.text("a"));
    readEvents.add(ReadEvent.slot());
    readEvents.add(ReadEvent.number(1));
    readEvents.add(ReadEvent.text("b"));
    readEvents.add(ReadEvent.slot());
    readEvents.add(ReadEvent.number(2));
    readEvents.add(ReadEvent.endRecord());

    readEvents.add(ReadEvent.endRecord());

    for (ReadEvent event : readEvents) {
      recognizer = (OuterClassRecognizer) recognizer.feedEvent(event);
      if (recognizer.isError()) {
        throw recognizer.trap();
      }
    }

    if (!recognizer.isDone()) {
      throw new RuntimeException("Recognizer did not complete");
    }
    if (recognizer.isError()) {
      throw recognizer.trap();
    }

    System.out.println(recognizer.bind());
  }

  static class InnerClassRecognizer extends Recognizer<InnerPropClass> {

    private Recognizer<InnerPropClass> recognizer;

    public InnerClassRecognizer() {
      this.recognizer = new ClassRecognizerInit<>(new FixedTagSpec(InnerPropClass.class.getSimpleName()), new InnerPropClassBuilder(), 2, (key) -> {
        if (key.isItem()) {
          ItemFieldKey itemFieldKey = (ItemFieldKey) key;
          switch (itemFieldKey.getName()) {
            case "a":
              return 0;
            case "b":
              return 1;
            default:
              throw new RuntimeException("Unexpected key: " + key);
          }
        }
        return null;
      });
    }

    @Override
    public Recognizer<InnerPropClass> feedEvent(ReadEvent event) {
      this.recognizer = this.recognizer.feedEvent(event);
      return this;
    }

    @Override
    public boolean isCont() {
      return this.recognizer.isCont();
    }

    @Override
    public boolean isDone() {
      return this.recognizer.isDone();
    }

    @Override
    public boolean isError() {
      return this.recognizer.isError();
    }

    @Override
    public InnerPropClass bind() {
      return this.recognizer.bind();
    }

    @Override
    public RuntimeException trap() {
      return this.recognizer.trap();
    }

    @Override
    public Recognizer<InnerPropClass> reset() {
      return null;
    }

  }

  static class InnerPropClass {

    private final int a;
    private final int b;

    public InnerPropClass(int a, int b) {
      this.a = a;
      this.b = b;
    }

    @Override
    public String toString() {
      return "InnerPropClass{" + "a=" + a + ", b=" + b + '}';
    }

  }


  static class InnerPropClassBuilder implements RecognizingBuilder<InnerPropClass> {

    private final FieldRecognizingBuilder<Integer> aBuilder = new FieldRecognizingBuilder<>(Integer.class);
    private final FieldRecognizingBuilder<Integer> bBuilder = new FieldRecognizingBuilder<>(Integer.class);

    @Override
    public boolean feedIndexed(int index, ReadEvent event) {
      switch (index) {
        case 0:
          return this.aBuilder.feed(event);
        case 1:
          return this.bBuilder.feed(event);
        default:
          throw new RuntimeException("Unknown idx: " + index);
      }
    }

    @Override
    public InnerPropClass bind() {
      return new InnerPropClass(this.aBuilder.bind(), this.bBuilder.bind());
    }

  }

  static class OuterPropClass {

    private String c;
    public InnerPropClass d;

    public OuterPropClass() {

    }

    @Override
    public String toString() {
      return "OuterPropClass{" +
          "c='" + c + '\'' +
          ", d=" + d +
          '}';
    }

    public void setC(String c) {
      this.c = c;
    }
  }

  static class OuterPropClassBuilder implements RecognizingBuilder<OuterPropClass> {

    private final FieldRecognizingBuilder<String> cBuilder = new FieldRecognizingBuilder<>(String.class);
    private final FieldRecognizingBuilder<InnerPropClass> dBuilder = new FieldRecognizingBuilder<>(new InnerClassRecognizer());

    @Override
    public boolean feedIndexed(int index, ReadEvent event) {
      switch (index) {
        case 0:
          return this.cBuilder.feed(event);
        case 1:
          return this.dBuilder.feed(event);
        default:
          throw new RuntimeException("Unknown idx: " + index);
      }
    }

    @Override
    public OuterPropClass bind() {
      OuterPropClass outerPropClass = new OuterPropClass();
      outerPropClass.setC(this.cBuilder.bind());
      outerPropClass.d = this.dBuilder.bind();

      return outerPropClass;
    }

  }

  static class OuterClassRecognizer extends Recognizer<OuterPropClass> {
    public Recognizer<OuterPropClass> recognizer;

    public OuterClassRecognizer() {
      this.recognizer = new ClassRecognizerInit<>(new FixedTagSpec(OuterPropClass.class.getSimpleName()), new OuterPropClassBuilder(), 2, (key) -> {
        if (key.isItem()) {
          ItemFieldKey itemFieldKey = (ItemFieldKey) key;
          switch (itemFieldKey.getName()) {
            case "c":
              return 0;
            case "d":
              return 1;
            default:
              throw new RuntimeException("Unexpected key: " + key);
          }
        }
        return null;
      });
    }

    private OuterClassRecognizer(Recognizer<OuterPropClass> recognizer) {
      this.recognizer = recognizer;
    }

    @Override
    public Recognizer<OuterPropClass> feedEvent(ReadEvent event) {
      this.recognizer = this.recognizer.feedEvent(event);
      return this;
    }

    @Override
    public boolean isCont() {
      return this.recognizer.isCont();
    }

    @Override
    public boolean isDone() {
      return this.recognizer.isDone();
    }

    @Override
    public boolean isError() {
      return this.recognizer.isError();
    }

    @Override
    public OuterPropClass bind() {
      return this.recognizer.bind();
    }

    @Override
    public RuntimeException trap() {
      return this.recognizer.trap();
    }

    @Override
    public Recognizer<OuterPropClass> reset() {
      return new OuterClassRecognizer(this.recognizer.reset());
    }

  }

}