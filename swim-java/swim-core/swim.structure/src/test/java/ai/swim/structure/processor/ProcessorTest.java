package ai.swim.structure.processor;

import ai.swim.recon.event.ReadEvent;
import ai.swim.structure.FieldRecognizingBuilder;
import ai.swim.structure.RecognizingBuilder;
import ai.swim.structure.annotations.AutoForm;
import ai.swim.structure.annotations.AutoloadedRecognizer;
import ai.swim.structure.recognizer.Recognizer;
import ai.swim.structure.recognizer.ScalarRecognizer;
import ai.swim.structure.recognizer.structural.labelled.LabelledClassRecognizer;
import ai.swim.structure.recognizer.structural.labelled.LabelledFieldKey;
import ai.swim.structure.recognizer.structural.tag.FixedTagSpec;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

import static ai.swim.structure.RecognizerTestUtil.runTest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProcessorTest {

  @AutoForm
  public static class SimpleClass {
    private String a;
    public int b;

    public SimpleClass() {

    }

    public SimpleClass(String a, int b) {
      this.a = a;
      this.b = b;
    }

    public void setA(String a) {
      this.a = a;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof SimpleClass)) return false;
      SimpleClass that = (SimpleClass) o;
      return b == that.b && Objects.equals(a, that.a);
    }

    @Override
    public int hashCode() {
      return Objects.hash(a, b);
    }

    @Override
    public String toString() {
      return "SimpleClass{" +
          "a='" + a + '\'' +
          ", b=" + b +
          '}';
    }
  }

  @Test
  void readSimpleClass() {
    Recognizer<SimpleClass> recognizer = new SimpleClassRecognizer();
    List<ReadEvent> events = List.of(
        ReadEvent.startAttribute("SimpleClass"),
        ReadEvent.extant(),
        ReadEvent.endAttribute(),
        ReadEvent.startBody(),
        ReadEvent.text("a"),
        ReadEvent.slot(),
        ReadEvent.text("valueA"),
        ReadEvent.text("b"),
        ReadEvent.slot(),
        ReadEvent.number(2),
        ReadEvent.endRecord()
    );

    SimpleClass simpleClass = runTest(recognizer, events);
    SimpleClass expected = new SimpleClass("valueA", 2);

    assertEquals(simpleClass, expected);
  }

  @AutoForm
  public static class SimpleClassSkippedField {
    @AutoForm.Ignore
    private int a = 0;
    private String b;

    public SimpleClassSkippedField() {

    }

    public SimpleClassSkippedField(String b) {
      this.b = b;
    }

    public void setA(int a) {
      this.a = a;
    }

    public void setB(String b) {
      this.b = b;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof SimpleClassSkippedField)) return false;
      SimpleClassSkippedField that = (SimpleClassSkippedField) o;
      return Objects.equals(b, that.b);
    }

    @Override
    public int hashCode() {
      return Objects.hash(a, b);
    }

    @Override
    public String toString() {
      return "SimpleClassSkippedField{" +
          "a=" + a +
          ", b='" + b + '\'' +
          '}';
    }
  }

  @Test
  void readSimpleClassSkippedFieldOk() {
    Recognizer<SimpleClassSkippedField> recognizer = new SimpleClassSkippedFieldRecognizer();
    List<ReadEvent> events = List.of(
        ReadEvent.startAttribute("SimpleClassSkippedField"),
        ReadEvent.extant(),
        ReadEvent.endAttribute(),
        ReadEvent.startBody(),
        ReadEvent.text("b"),
        ReadEvent.slot(),
        ReadEvent.text("valueA"),
        ReadEvent.endRecord()
    );

    SimpleClassSkippedField simpleClass = runTest(recognizer, events);
    SimpleClassSkippedField expected = new SimpleClassSkippedField("valueA");

    assertEquals(simpleClass, expected);
  }

  @Test
  void readSimpleClassSkippedFieldErr() {
    Recognizer<SimpleClassSkippedField> recognizer = new SimpleClassSkippedFieldRecognizer();
    List<ReadEvent> events = List.of(
        ReadEvent.startAttribute("SimpleClassSkippedField"),
        ReadEvent.extant(),
        ReadEvent.endAttribute(),
        ReadEvent.startBody(),
        ReadEvent.text("a"),
        ReadEvent.slot(),
        ReadEvent.number(1),
        ReadEvent.text("b"),
        ReadEvent.slot(),
        ReadEvent.text("valueA"),
        ReadEvent.endRecord()
    );

    try {
      runTest(recognizer, events);
    } catch (RuntimeException e) {
      assertTrue(e.getMessage().startsWith("Unexpected key"));
    }
  }

  @AutoForm
  public static class ListClass {
    private List<Integer> list;
    private int number;

    public ListClass() {
    }

    public ListClass(List<Integer> list, int number) {
      this.list = list;
      this.number = number;
    }

    public void setList(List<Integer> list) {
      this.list = list;
    }

    public void setNumber(int number) {
      this.number = number;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof ListClass)) return false;
      ListClass listClass = (ListClass) o;
      return number == listClass.number && Objects.equals(list, listClass.list);
    }

    @Override
    public int hashCode() {
      return Objects.hash(list, number);
    }

    @Override
    public String toString() {
      return "ListClass{" +
          "list=" + list +
          ", number=" + number +
          '}';
    }
  }

  @Test
  void readListClass() {
    Recognizer<ListClass> recognizer = new ListClassRecognizer();
    List<ReadEvent> events = List.of(
        ReadEvent.startAttribute("ListClass"),
        ReadEvent.extant(),
        ReadEvent.endAttribute(),
        ReadEvent.startBody(),
        ReadEvent.text("list"),
        ReadEvent.slot(),
        ReadEvent.startBody(),
        ReadEvent.number(1),
        ReadEvent.number(2),
        ReadEvent.number(3),
        ReadEvent.endRecord(),
        ReadEvent.text("number"),
        ReadEvent.slot(),
        ReadEvent.number(4),
        ReadEvent.endRecord()
    );

    ListClass listClass = runTest(recognizer, events);
    ListClass expected = new ListClass(List.of(1, 2, 3), 4);

    assertEquals(listClass, expected);
  }

  @AutoForm
  public static class ListOfClasses {
    private List<SimpleClass> classes;

    public ListOfClasses() {

    }

    public ListOfClasses(List<SimpleClass> classes) {
      this.classes = classes;
    }

    public void setClasses(List<SimpleClass> classes) {
      this.classes = classes;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof ListOfClasses)) return false;
      ListOfClasses that = (ListOfClasses) o;
      return Objects.equals(classes, that.classes);
    }

    @Override
    public String toString() {
      return "ListOfClasses{" +
          "classes=" + classes +
          '}';
    }

    @Override
    public int hashCode() {
      return Objects.hash(classes);
    }
  }

  @Test
  void readListOfClasses() {
    Recognizer<ListOfClasses> recognizer = new ListOfClassesRecognizer();
    List<ReadEvent> events = List.of(
        ReadEvent.startAttribute("ListOfClasses"),
        ReadEvent.extant(),
        ReadEvent.endAttribute(),
        ReadEvent.startBody(),
        ReadEvent.text("classes"),
        ReadEvent.slot(),

        ReadEvent.startAttribute("SimpleClass"),
        ReadEvent.extant(),
        ReadEvent.endAttribute(),
        ReadEvent.startBody(),
        ReadEvent.text("a"),
        ReadEvent.slot(),
        ReadEvent.text("valueA"),
        ReadEvent.text("b"),
        ReadEvent.slot(),
        ReadEvent.number(2),
        ReadEvent.endRecord(),

        ReadEvent.startAttribute("SimpleClass"),
        ReadEvent.extant(),
        ReadEvent.endAttribute(),
        ReadEvent.startBody(),
        ReadEvent.text("a"),
        ReadEvent.slot(),
        ReadEvent.text("valueA"),
        ReadEvent.text("b"),
        ReadEvent.slot(),
        ReadEvent.number(2),
        ReadEvent.endRecord(),

        ReadEvent.startAttribute("SimpleClass"),
        ReadEvent.extant(),
        ReadEvent.endAttribute(),
        ReadEvent.startBody(),
        ReadEvent.text("a"),
        ReadEvent.slot(),
        ReadEvent.text("valueA"),
        ReadEvent.text("b"),
        ReadEvent.slot(),
        ReadEvent.number(2),
        ReadEvent.endRecord(),

        ReadEvent.endRecord()
    );

    ListOfClasses listClass = runTest(recognizer, events);
    ListOfClasses expected = new ListOfClasses();

    assertEquals(listClass, expected);
  }

  public static class WrittenDependant {
    private String a;

    public WrittenDependant() {

    }

    public WrittenDependant(String a) {
      this.a = a;
    }

    public void setA(String a) {
      this.a = a;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof WrittenDependant)) return false;
      WrittenDependant writtenDependant = (WrittenDependant) o;
      return Objects.equals(a, writtenDependant.a);
    }

    @Override
    public int hashCode() {
      return Objects.hash(a);
    }

    @Override
    public String toString() {
      return "WrittenDependant{" +
          "a='" + a + '\'' +
          '}';
    }
  }

  // Class is dynamically loaded by the recognizer proxy
  @SuppressWarnings("unused")
  @AutoloadedRecognizer(WrittenDependant.class)
  public static final class WrittenDependantRecognizer extends Recognizer<WrittenDependant> {
    private Recognizer<WrittenDependant> recognizer;

    public WrittenDependantRecognizer() {
      this.recognizer = new LabelledClassRecognizer<>(new FixedTagSpec(WrittenDependant.class.getSimpleName()), new WrittenDependantBuilder(), 1, (key) -> {
        if (key.isItem()) {
          LabelledFieldKey.ItemFieldKey itemFieldKey = (LabelledFieldKey.ItemFieldKey) key;
          if ("a".equals(itemFieldKey.getName())) {
            return 0;
          }
          throw new RuntimeException("Unexpected key: " + key);
        }
        return null;
      });
    }

    private WrittenDependantRecognizer(Recognizer<WrittenDependant> recognizer) {
      this.recognizer = recognizer;
    }

    @Override
    public Recognizer<WrittenDependant> feedEvent(ReadEvent event) {
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
    public WrittenDependant bind() {
      return this.recognizer.bind();
    }

    @Override
    public RuntimeException trap() {
      return this.recognizer.trap();
    }

    @Override
    public Recognizer<WrittenDependant> reset() {
      return new WrittenDependantRecognizer(this.recognizer.reset());
    }
  }

  public static final class WrittenDependantBuilder implements RecognizingBuilder<WrittenDependant> {
    private RecognizingBuilder<String> aBuilder = new FieldRecognizingBuilder<>(ScalarRecognizer.STRING);

    public WrittenDependantBuilder() {
    }

    @Override
    public boolean feedIndexed(int index, ReadEvent event) {
      if (index == 0) {
        return this.aBuilder.feed(event);
      }
      throw new RuntimeException("Unknown idx: " + index);
    }

    @Override
    public WrittenDependant bind() {
      WrittenDependant obj = new WrittenDependant();

      obj.setA(this.aBuilder.bind());

      return obj;
    }

    @Override
    public RecognizingBuilder<WrittenDependant> reset() {
      this.aBuilder = this.aBuilder.reset();
      return this;
    }
  }

  @AutoForm
  public static class Dependant {
    private WrittenDependant writtenDependant;

    public Dependant() {

    }

    public Dependant(WrittenDependant writtenDependant) {
      this.writtenDependant = writtenDependant;
    }

    public void setWrittenDependant(WrittenDependant writtenDependant) {
      this.writtenDependant = writtenDependant;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof Dependant)) return false;
      Dependant that = (Dependant) o;
      return Objects.equals(writtenDependant, that.writtenDependant);
    }

    @Override
    public int hashCode() {
      return Objects.hash(writtenDependant);
    }

    @Override
    public String toString() {
      return "Dependant{" +
          "writtenDependant=" + writtenDependant +
          '}';
    }
  }

  /**
   * The derived dependant builder will have a field that dynamically looks up the type of WrittenDependant through
   * the recognizer proxy. For this test to succeed, the annotation processor will have to have failed to have looked it
   * up and will instead write this proxy lookup instead.
   */
  @Test
  void testWrittenDependant() {
    Recognizer<Dependant> recognizer = new DependantRecognizer();
    List<ReadEvent> readEvents = List.of(
        ReadEvent.startAttribute("Dependant"),
        ReadEvent.endAttribute(),
        ReadEvent.startBody(),
        ReadEvent.text("writtenDependant"),
        ReadEvent.slot(),
        ReadEvent.startAttribute("WrittenDependant"),
        ReadEvent.endAttribute(),
        ReadEvent.startBody(),
        ReadEvent.text("a"),
        ReadEvent.slot(),
        ReadEvent.text("stringy"),
        ReadEvent.endRecord(),
        ReadEvent.endRecord()
    );

    Dependant actual = runTest(recognizer, readEvents);
    Dependant expected = new Dependant(new WrittenDependant("stringy"));
    assertEquals(actual, expected);
  }

  @AutoForm
  public static class RenamedFieldClass {
    @AutoForm.Name("renamed_field")
    private int a;

    public RenamedFieldClass() {

    }

    public RenamedFieldClass(int a) {
      this.a = a;
    }

    public void setA(int a) {
      this.a = a;
    }

    @Override
    public String toString() {
      return "RenamedFieldClass{" +
          "a=" + a +
          '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof RenamedFieldClass)) return false;
      RenamedFieldClass that = (RenamedFieldClass) o;
      return a == that.a;
    }

    @Override
    public int hashCode() {
      return Objects.hash(a);
    }
  }

  @Test
  void testRenamedField() {
    Recognizer<RenamedFieldClass> recognizer = new RenamedFieldClassRecognizer();
    List<ReadEvent> events = List.of(
        ReadEvent.startAttribute("RenamedFieldClass"),
        ReadEvent.extant(),
        ReadEvent.endAttribute(),
        ReadEvent.startBody(),
        ReadEvent.text("renamed_field"),
        ReadEvent.slot(),
        ReadEvent.number(13),
        ReadEvent.endRecord()
    );

    RenamedFieldClass obj = runTest(recognizer, events);
    RenamedFieldClass expected = new RenamedFieldClass(13);

    assertEquals(obj, expected);
  }

  @AutoForm(tag = "tag")
  public static class TagClass {
    private int a;

    public TagClass() {

    }

    public TagClass(int a) {
      this.a = a;
    }

    public void setA(int a) {
      this.a = a;
    }

    @Override
    public String toString() {
      return "TagClass{" +
          "a=" + a +
          '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof TagClass)) return false;
      TagClass that = (TagClass) o;
      return a == that.a;
    }

    @Override
    public int hashCode() {
      return Objects.hash(a);
    }
  }

  @Test
  void testTagOk() {
    Recognizer<TagClass> recognizer = new TagClassRecognizer();
    List<ReadEvent> events = List.of(
        ReadEvent.startAttribute("tag"),
        ReadEvent.extant(),
        ReadEvent.endAttribute(),
        ReadEvent.startBody(),
        ReadEvent.text("a"),
        ReadEvent.slot(),
        ReadEvent.number(13),
        ReadEvent.endRecord()
    );

    TagClass obj = runTest(recognizer, events);
    TagClass expected = new TagClass(13);

    assertEquals(obj, expected);
  }

  @Test
  void testTagMismatch() {
    Recognizer<TagClass> recognizer = new TagClassRecognizer();
    List<ReadEvent> events = List.of(
        ReadEvent.startAttribute("TagClass"),
        ReadEvent.extant(),
        ReadEvent.endAttribute(),
        ReadEvent.startBody(),
        ReadEvent.text("a"),
        ReadEvent.slot(),
        ReadEvent.number(13),
        ReadEvent.endRecord()
    );

    try {
      runTest(recognizer, events);
    } catch (RuntimeException e) {
      assertEquals(e.getMessage(), "Unexpected attribute: TagClass");
    }
  }

  @AutoForm
  public static class OptionalFieldClass {
    @AutoForm.Optional
    private int a;
    public String b;

    public OptionalFieldClass() {

    }

    public OptionalFieldClass(String b) {
      this.b = b;
    }

    public OptionalFieldClass(int a, String b) {
      this.a = a;
      this.b = b;
    }

    public void setA(int a) {
      this.a = a;
    }

    @Override
    public String toString() {
      return "OptionalFieldClass{" +
          "a=" + a +
          ", b='" + b + '\'' +
          '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof OptionalFieldClass)) return false;
      OptionalFieldClass that = (OptionalFieldClass) o;
      return a == that.a && Objects.equals(b, that.b);
    }

    @Override
    public int hashCode() {
      return Objects.hash(a, b);
    }
  }

  @Test
  void testOptionalWithout() {
    Recognizer<OptionalFieldClass> recognizer = new OptionalFieldClassRecognizer();

    List<ReadEvent> events = List.of(
        ReadEvent.startAttribute("OptionalFieldClass"),
        ReadEvent.extant(),
        ReadEvent.endAttribute(),
        ReadEvent.startBody(),
        ReadEvent.text("b"),
        ReadEvent.slot(),
        ReadEvent.text("value"),
        ReadEvent.endRecord()
    );

    OptionalFieldClass optionalFieldClass = runTest(recognizer, events);
    OptionalFieldClass expected = new OptionalFieldClass("value");

    assertEquals(optionalFieldClass, expected);
  }

  @Test
  void testOptionalWith() {
    Recognizer<OptionalFieldClass> recognizer = new OptionalFieldClassRecognizer();

    List<ReadEvent> events = List.of(
        ReadEvent.startAttribute("OptionalFieldClass"),
        ReadEvent.extant(),
        ReadEvent.endAttribute(),
        ReadEvent.startBody(),
        ReadEvent.text("a"),
        ReadEvent.slot(),
        ReadEvent.number(1),
        ReadEvent.text("b"),
        ReadEvent.slot(),
        ReadEvent.text("value"),
        ReadEvent.endRecord()
    );

    OptionalFieldClass optionalFieldClass = runTest(recognizer, events);
    OptionalFieldClass expected = new OptionalFieldClass(1, "value");

    assertEquals(optionalFieldClass, expected);
  }
}
