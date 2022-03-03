package ai.swim.structure.form;

import ai.swim.structure.form.event.ReadEvent;
import ai.swim.structure.form.recognizer.Recognizer;
import ai.swim.structure.form.recognizer.RecognizerProxy;
import ai.swim.structure.form.recognizer.structural.ClassRecognizerInit;
import ai.swim.structure.form.recognizer.structural.LabelledVTable;
import ai.swim.structure.form.recognizer.structural.key.ItemFieldKey;
import ai.swim.structure.form.recognizer.structural.tag.FixedTagSpec;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

class ClassRecognizerTest {

  static class InnerClassRecognizer extends Recognizer<InnerPropClass> {
    private Recognizer<InnerPropClass> recognizer;

    public InnerClassRecognizer() {
      this.recognizer = new ClassRecognizerInit<>(new FixedTagSpec(InnerPropClass.class.getSimpleName()), new InnerPropClassBuilder(), 2, new LabelledVTable<>((key) -> {
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
      }, (Builder::feed), (Builder::bind), (builder -> {
        throw new RuntimeException("On done");
      })));
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
    public Exception trap() {
      return this.recognizer.trap();
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

  static class FieldBuilder<I> implements Builder<I> {
    public final Recognizer<I> recognizer;
    public I value;

    public FieldBuilder(Class<I> clazz) {
      this.recognizer = RecognizerProxy.lookup(clazz);
    }

    public FieldBuilder(Recognizer<I> recognizer) {
      this.recognizer = recognizer;
    }

    @Override
    public boolean feed(Integer index, ReadEvent event) {
      if (this.value != null) {
        throw new RuntimeException("Duplicate value");
      }

      Recognizer<I> feedResult = this.recognizer.feedEvent(event);
      if (feedResult.isDone()) {
        value = feedResult.bind();
        return true;
      } else if (feedResult.isError()) {
        throw (RuntimeException) feedResult.trap();
      } else {
        return false;
      }
    }

    @Override
    public I bind() {
      return Objects.requireNonNull(this.value);
    }
  }

  static class InnerPropClassBuilder implements Builder<InnerPropClass> {
    private final FieldBuilder<Integer> aBuilder = new FieldBuilder<>(Integer.class);
    private final FieldBuilder<Integer> bBuilder = new FieldBuilder<>(Integer.class);

    @Override
    public boolean feed(Integer index, ReadEvent event) {
      switch (index) {
        case 0:
          return this.aBuilder.feed(null, event);
        case 1:
          return this.bBuilder.feed(null, event);
        default:
          throw new RuntimeException("Unknown idx: " + index);
      }
    }

    @Override
    public InnerPropClass bind() {
      return new InnerPropClass(this.aBuilder.bind(), this.bBuilder.bind());
    }
  }

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

  static class OuterPropClass {
    private final String c;
    private final InnerPropClass d;

    public OuterPropClass(String c, InnerPropClass d) {
      this.c = c;
      this.d = d;
    }

    @Override
    public String toString() {
      return "OuterPropClass{" +
          "c='" + c + '\'' +
          ", d=" + d +
          '}';
    }
  }

  static class OuterPropClassBuilder implements Builder<OuterPropClass> {
    private final FieldBuilder<String> cBuilder = new FieldBuilder<>(String.class);
    private final FieldBuilder<InnerPropClass> dBuilder = new FieldBuilder<>(new InnerClassRecognizer());

    @Override
    public boolean feed(Integer index, ReadEvent event) {
      switch (index) {
        case 0:
          return this.cBuilder.feed(null, event);
        case 1:
          return this.dBuilder.feed(null, event);
        default:
          throw new RuntimeException("Unknown idx: " + index);
      }
    }

    @Override
    public OuterPropClass bind() {
      return new OuterPropClass(this.cBuilder.bind(), this.dBuilder.bind());
    }
  }

  static class OuterClassRecognizer extends Recognizer<OuterPropClass> {
    public Recognizer<OuterPropClass> recognizer;

    public OuterClassRecognizer() {
      this.recognizer = new ClassRecognizerInit<>(new FixedTagSpec(OuterPropClass.class.getSimpleName()), new OuterPropClassBuilder(), 2, new LabelledVTable<>((key) -> {
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
      }, (Builder::feed), (Builder::bind), (builder -> {
        throw new RuntimeException("On done");
      })));
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
    public Exception trap() {
      return this.recognizer.trap();
    }
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
      System.out.println("Feed event: " + event + ", state: " + recognizer.recognizer.getClass().getSimpleName());

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

}