package ai.swim.structure.processor;

import ai.swim.recon.event.ReadEvent;
import ai.swim.structure.annotations.AutoForm;
import ai.swim.structure.recognizer.Recognizer;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

import static ai.swim.structure.RecognizerTestUtil.runTest;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
    private final int a = 0;
    private String b;

    public SimpleClassSkippedField() {

    }

    public SimpleClassSkippedField(String b) {
      this.b = b;
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
  void readSimpleClassSkippedField() {
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
  void readListOfClass() {
    Recognizer<ListOfClasses>recognizer = new ListOfClassesRecognizer();
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
}
