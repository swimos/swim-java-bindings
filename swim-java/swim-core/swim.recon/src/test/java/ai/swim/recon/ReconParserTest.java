package ai.swim.recon;

import ai.swim.codec.input.Input;
import ai.swim.recon.event.ReadEvent;
import ai.swim.recon.result.ParseResult;
import ai.swim.recon.result.ResultError;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReconParserTest {

  void runTestOk(String string, List<ReadEvent> expected) {
    testCompleteOk(string, expected);
    testIncrementalOk(string, expected);
  }

  private void testIncrementalOk(String string, List<ReadEvent> expected) {
    ReconParser parser = new ReconParser(Input.string(String.valueOf(string.charAt(0))).isPartial(true));
    List<ReadEvent> actual = new ArrayList<>(expected.size());

    for (int i = 1; i < string.length(); i++) {
      assertTrue(parser.isCont());
      ParseResult<ReadEvent> parseResult = parser.next();
      if (parseResult.isError()) {
        fail(((ResultError<?>) parseResult).getCause());
      }

      if (parseResult.isOk()) {
        actual.add(parseResult.bind());
      }

      String s = String.valueOf(string.charAt(i));
      boolean isPartial = i != string.length() - 1;

      parser = parser.feed(Input.string(s).isPartial(isPartial));
    }

    // A sufficiently large enough loop to drain any pending events but not cause an infinite loop
    for (int i = 0; i < 1000; i++) {
      ParseResult<ReadEvent> parseResult = parser.next();
      if (parseResult.isError()) {
        fail(((ResultError<?>) parseResult).getCause());
      }

      if (parseResult.isDone()) {
        break;
      }

      assertTrue(parseResult.isOk());
      actual.add(parseResult.bind());

      if (!parser.hasEvents()) {
        break;
      }
    }

    assertTrue(parser.isDone());
    assertEquals(expected, actual);
  }

  void testCompleteOk(String input, List<ReadEvent> expected) {
    ReconParser parser = new ReconParser(Input.string(input));

//    for (int i = 0; i < 20; i++) {
//      System.out.println(parser.next());
//    }

    for (ReadEvent event : expected) {
      assertEquals(ParseResult.ok(event), parser.next());
    }

    assertEquals(ParseResult.end(), parser.next());
  }

  @Test
  void testParser() {
    runTestOk("@tag(field:1)", List.of(
        ReadEvent.startAttribute("tag"),
        ReadEvent.text("field"),
        ReadEvent.slot(),
        ReadEvent.number(1),
        ReadEvent.endAttribute(),
        ReadEvent.startBody(),
        ReadEvent.endRecord()
    ));
    runTestOk("@tag(a:1,b:abc,c:\"string\")", List.of(
        ReadEvent.startAttribute("tag"),
        ReadEvent.text("a"),
        ReadEvent.slot(),
        ReadEvent.number(1),
        ReadEvent.text("b"),
        ReadEvent.slot(),
        ReadEvent.text("abc"),
        ReadEvent.text("c"),
        ReadEvent.slot(),
        ReadEvent.text("string"),
        ReadEvent.endAttribute(),
        ReadEvent.startBody(),
        ReadEvent.endRecord()
    ));

    runTestOk("@tag(a:\"abc\")",
        List.of(
            ReadEvent.startAttribute("tag"),
            ReadEvent.text("a"),
            ReadEvent.slot(),
            ReadEvent.text("abc"),
            ReadEvent.endAttribute(),
            ReadEvent.startBody(),
            ReadEvent.endRecord()
        )
    );

    runTestOk("\"string\"", List.of(ReadEvent.text("string")));
  }

  @Test
  void emptyInput() {
    ReconParser parser = new ReconParser(Input.string(""));
    assertEquals(ParseResult.ok(ReadEvent.extant()), parser.next());
    assertEquals(ParseResult.end(), parser.next());
    assertEquals(ParseResult.end(), parser.next());
  }

  @Test
  void emptyRecords() {
    List<ReadEvent> events = List.of(ReadEvent.startBody(), ReadEvent.endRecord());
    runTestOk("{}", events);
    runTestOk("{ }", events);
    runTestOk("{\n}", events);
    runTestOk("{\r\n}", events);
  }

  @Test
  void singletonRecord() {
    List<ReadEvent> events = List.of(ReadEvent.startBody(), ReadEvent.number(1), ReadEvent.endRecord());
    runTestOk("{1}", events);
    runTestOk("{ 1 }", events);
    runTestOk("{\n 1}", events);
    runTestOk("{\r\n 1}", events);
  }

  @Test
  void simpleRecord() {
    List<ReadEvent> events = List.of(
        ReadEvent.startBody(),
        ReadEvent.number(1),
        ReadEvent.text("two"),
        ReadEvent.number(3),
        ReadEvent.endRecord()
    );

    runTestOk("{1,two,3}", events);
    runTestOk("{ 1, two, 3}", events);
    runTestOk(" { 1, two, 3 }", events);
  }

  @Test
  void missingItems() {
    runTestOk("{,two,3}", List.of(
        ReadEvent.startBody(),
        ReadEvent.extant(),
        ReadEvent.text("two"),
        ReadEvent.number(3),
        ReadEvent.endRecord()
    ));
    runTestOk("{1,,3}", List.of(
        ReadEvent.startBody(),
        ReadEvent.number(1),
        ReadEvent.extant(),
        ReadEvent.number(3),
        ReadEvent.endRecord()
    ));
    runTestOk("{1,two,}", List.of(
        ReadEvent.startBody(),
        ReadEvent.number(1),
        ReadEvent.text("two"),
        ReadEvent.extant(),
        ReadEvent.endRecord()
    ));
  }

  @Test
  void newlineSeparators() {
    List<ReadEvent> events = List.of(
        ReadEvent.startBody(),
        ReadEvent.number(1),
        ReadEvent.text("two"),
        ReadEvent.number(3),
        ReadEvent.endRecord()
    );

    runTestOk("{1\ntwo\n3}", events);
    runTestOk("{\n\t1\n\ttwo\n\t3\n}", events);
  }

  @Test
  void singletonSlot() {
    List<ReadEvent> events = List.of(
        ReadEvent.startBody(),
        ReadEvent.text("name"),
        ReadEvent.slot(),
        ReadEvent.number(1),
        ReadEvent.endRecord()
    );

    runTestOk("{name:1}", events);
    runTestOk("{ name: 1 }", events);
    runTestOk("{\nname: 1 }", events);
  }

  @Test
  void missingSlotValue() {
    List<ReadEvent> events = List.of(
        ReadEvent.startBody(),
        ReadEvent.text("name"),
        ReadEvent.slot(),
        ReadEvent.extant(),
        ReadEvent.endRecord()
    );

    runTestOk("{name:}", events);
    runTestOk("{ name: }", events);
    runTestOk("{\n name:\n }", events);
  }

  @Test
  void missingSlotKey() {
    List<ReadEvent> events = List.of(
        ReadEvent.startBody(),
        ReadEvent.extant(),
        ReadEvent.slot(),
        ReadEvent.number(1),
        ReadEvent.endRecord()
    );

    runTestOk("{:1}", events);
    runTestOk("{ : 1 }", events);
    runTestOk("{\n : 1 }", events);
  }

  @Test
  void simpleSlotsRecord() {
    List<ReadEvent> events = List.of(
        ReadEvent.startBody(),
        ReadEvent.text("first"),
        ReadEvent.slot(),
        ReadEvent.number(1),
        ReadEvent.text("second"),
        ReadEvent.slot(),
        ReadEvent.text("two"),
        ReadEvent.text("third"),
        ReadEvent.slot(),
        ReadEvent.number(3),
        ReadEvent.endRecord()
    );

    runTestOk("{first:1,second:two,third:3}", events);
    runTestOk("{ first: 1, second: two, third: 3 }", events);
  }

  @Test
  void missingSlotParts() {
    List<ReadEvent> events = List.of(
        ReadEvent.startBody(),
        ReadEvent.text("first"),
        ReadEvent.slot(),
        ReadEvent.number(1),
        ReadEvent.text("second"),
        ReadEvent.slot(),
        ReadEvent.extant(),
        ReadEvent.extant(),
        ReadEvent.slot(),
        ReadEvent.number(3),
        ReadEvent.endRecord()
    );

    runTestOk("{first:1,second:,:3}", events);
    runTestOk("{ first: 1, second: , : 3 }", events);
    runTestOk("{first:1,second:\n:3}", events);
    runTestOk("{first:1\nsecond:\n:3\n}", events);
    runTestOk("{first:1,second:\r\n:3}", events);
    runTestOk("{first:1\r\nsecond:\r\n:3\r\n}", events);
  }

  @Test
  void tagAttribute() {
    List<ReadEvent> events = List.of(
        ReadEvent.startAttribute("tag"),
        ReadEvent.endAttribute(),
        ReadEvent.startBody(),
        ReadEvent.endRecord()
    );

    runTestOk("@tag", events);
    runTestOk("@tag {}", events);
  }

  @Test
  void attrSimpleBody() {
    List<ReadEvent> events = List.of(
        ReadEvent.startAttribute("name"),
        ReadEvent.number(2),
        ReadEvent.endAttribute(),
        ReadEvent.startBody(),
        ReadEvent.endRecord()
    );

    runTestOk("@name(2)", events);
    runTestOk("@name(2) {}", events);
  }

  @Test
  void attrSlotBody() {
    List<ReadEvent> events = List.of(
        ReadEvent.startAttribute("name"),
        ReadEvent.text("a"),
        ReadEvent.slot(),
        ReadEvent.bool(true),
        ReadEvent.endAttribute(),
        ReadEvent.startBody(),
        ReadEvent.endRecord()
    );

    runTestOk("@name(a:true)", events);
    runTestOk("@name(a:true) {}", events);
    runTestOk("@name(a:true\n) {}", events);
    runTestOk("@name(a:true\r\n) {}", events);
  }

  @Test
  void attrSlotBodyMissingParts() {
    List<ReadEvent> events = List.of(
        ReadEvent.startAttribute("name"),
        ReadEvent.text("a"),
        ReadEvent.slot(),
        ReadEvent.extant(),
        ReadEvent.text("b"),
        ReadEvent.slot(),
        ReadEvent.extant(),
        ReadEvent.text("c"),
        ReadEvent.slot(),
        ReadEvent.extant(),
        ReadEvent.endAttribute(),
        ReadEvent.startBody(),
        ReadEvent.endRecord()
    );

    runTestOk("@name(a:,b:,c:)", events);
    runTestOk("@name(a:\nb:\nc:\n)", events);
    runTestOk("@name(a:\r\nb:\r\nc:\r\n)", events);
    runTestOk("@name(a:\n\nb:\n\nc:\n\n)", events);
    runTestOk("@name(a:\r\n\r\nb:\r\n\r\nc:\r\n\r\n)", events);
  }

  @Test
  void attrMultipleItemBody() {
    List<ReadEvent> events = List.of(
        ReadEvent.startAttribute("name"),
        ReadEvent.number(1),
        ReadEvent.text("a"),
        ReadEvent.slot(),
        ReadEvent.bool(true),
        ReadEvent.extant(),
        ReadEvent.endAttribute(),
        ReadEvent.startBody(),
        ReadEvent.endRecord()
    );

    runTestOk("@name(1, a: true,)", events);
    runTestOk("@name(1, a: true,) {}", events);
    runTestOk("@name(1\n a: true,)", events);
    runTestOk("@name(1\r\n a: true,)", events);
  }

  @Test
  void multipleAttributes() {
    List<ReadEvent> events = List.of(
        ReadEvent.startAttribute("first"),
        ReadEvent.endAttribute(),
        ReadEvent.startAttribute("second"),
        ReadEvent.endAttribute(),
        ReadEvent.startBody(),
        ReadEvent.endRecord()
    );

    runTestOk("@first@second", events);
    runTestOk("@first@second {}", events);
  }

  @Test
  void multipleAttributesWithBodies() {
    List<ReadEvent> events = List.of(
        ReadEvent.startAttribute("first"),
        ReadEvent.number(1),
        ReadEvent.endAttribute(),
        ReadEvent.startAttribute("second"),
        ReadEvent.number(2),
        ReadEvent.endAttribute(),
        ReadEvent.startBody(),
        ReadEvent.endRecord()
    );

    runTestOk("@first(1)@second(2)", events);
    runTestOk("@first(1)@second(2) {}", events);
  }

  @Test
  void emptyNested() {
    List<ReadEvent> events = List.of(
        ReadEvent.startBody(),
        ReadEvent.startBody(),
        ReadEvent.endRecord(),
        ReadEvent.startBody(),
        ReadEvent.endRecord(),
        ReadEvent.startBody(),
        ReadEvent.endRecord(),
        ReadEvent.endRecord()
    );

    runTestOk("{{},{},{}}", events);
  }

  @Test
  void simpleNested() {
    List<ReadEvent> events = List.of(
        ReadEvent.startBody(),
        ReadEvent.startBody(),
        ReadEvent.number(4),
        ReadEvent.text("slot"),
        ReadEvent.slot(),
        ReadEvent.text("word"),
        ReadEvent.endRecord(),
        ReadEvent.number(1),
        ReadEvent.endRecord()
    );

    runTestOk("{\n" +
        "            { 4, slot: word }\n" +
        "            1\n" +
        "        }", events);
  }

  @Test
  void nestedWithAttr() {
    List<ReadEvent> events = List.of(
        ReadEvent.startBody(),
        ReadEvent.startAttribute("inner"),
        ReadEvent.endAttribute(),
        ReadEvent.startBody(),
        ReadEvent.endRecord(),
        ReadEvent.endRecord()
    );

    runTestOk("{ @inner }", events);
    runTestOk("{ @inner {} }", events);
  }

  @Test
  void nestedAttrWithBody() {
    List<ReadEvent> events = List.of(
        ReadEvent.startBody(),
        ReadEvent.startAttribute("inner"),
        ReadEvent.number(0),
        ReadEvent.endAttribute(),
        ReadEvent.startBody(),
        ReadEvent.endRecord(),
        ReadEvent.endRecord()
    );

    runTestOk("{ @inner(0) }", events);
    runTestOk("{ @inner(0) {} }", events);
  }

  @Test
  void nestedWithAttrWithBodyFollowed() {
    List<ReadEvent> events = List.of(
        ReadEvent.startBody(),
        ReadEvent.startAttribute("inner"),
        ReadEvent.number(0),
        ReadEvent.endAttribute(),
        ReadEvent.startBody(),
        ReadEvent.endRecord(),
        ReadEvent.text("after"),
        ReadEvent.endRecord()
    );

    runTestOk("{ @inner(0), after }", events);
    runTestOk("{ @inner(0) {}, after }", events);
  }

  @Test
  void emptyNestedInAttr() {
    List<ReadEvent> events = List.of(
        ReadEvent.startAttribute("outer"),
        ReadEvent.startBody(),
        ReadEvent.endRecord(),
        ReadEvent.endAttribute(),
        ReadEvent.startBody(),
        ReadEvent.endRecord()
    );

    runTestOk("@outer({})", events);
  }

  @Test
  void simpleNestedInAttr() {
    List<ReadEvent> events = List.of(
        ReadEvent.startAttribute("outer"),
        ReadEvent.startBody(),
        ReadEvent.number(4),
        ReadEvent.text("slot"),
        ReadEvent.slot(),
        ReadEvent.text("word"),
        ReadEvent.endRecord(),
        ReadEvent.endAttribute(),
        ReadEvent.startBody(),
        ReadEvent.endRecord()
    );

    runTestOk("@outer({ 4, slot: word })", events);
  }

  @Test
  void nestedWithAttrInAttr() {
    List<ReadEvent> events = List.of(
        ReadEvent.startAttribute("outer"),
        ReadEvent.startAttribute("inner"),
        ReadEvent.endAttribute(),
        ReadEvent.startBody(),
        ReadEvent.endRecord(),
        ReadEvent.endAttribute(),
        ReadEvent.startBody(),
        ReadEvent.endRecord()
    );

    runTestOk("@outer(@inner)", events);
    runTestOk("@outer(@inner {})", events);
  }

  @Test
  void nestedWithAttrWithBodyInAttr() {
    List<ReadEvent> events = List.of(
        ReadEvent.startAttribute("outer"),
        ReadEvent.startAttribute("inner"),
        ReadEvent.number(0),
        ReadEvent.endAttribute(),
        ReadEvent.startBody(),
        ReadEvent.endRecord(),
        ReadEvent.endAttribute(),
        ReadEvent.startBody(),
        ReadEvent.endRecord()
    );

    runTestOk("@outer(@inner(0))", events);
    runTestOk("@outer(@inner(0) {})", events);
  }

  @Test
  void nestedWithAttrWithBodyFollowedInAttr() {
    List<ReadEvent> events = List.of(
        ReadEvent.startAttribute("outer"),
        ReadEvent.startAttribute("inner"),
        ReadEvent.number(0),
        ReadEvent.endAttribute(),
        ReadEvent.startBody(),
        ReadEvent.endRecord(),
        ReadEvent.number(3),
        ReadEvent.endAttribute(),
        ReadEvent.startBody(),
        ReadEvent.endRecord()
    );

    runTestOk("@outer(@inner(0), 3)", events);
    runTestOk("@outer(@inner(0) {}, 3)", events);
  }

  @Test
  void doubleNested() {
    List<ReadEvent> events = List.of(
        ReadEvent.startBody(),
        ReadEvent.number(1),
        ReadEvent.startBody(),
        ReadEvent.number(2),
        ReadEvent.startBody(),
        ReadEvent.number(3),
        ReadEvent.number(4),
        ReadEvent.endRecord(),
        ReadEvent.endRecord(),
        ReadEvent.number(5),
        ReadEvent.endRecord()
    );

    runTestOk("{1, {2, {3, 4}}, 5}", events);
  }

  @Test
  void complexSlot() {
    List<ReadEvent> events = List.of(
        ReadEvent.startBody(),
        ReadEvent.startAttribute("key"),
        ReadEvent.endAttribute(),
        ReadEvent.startBody(),
        ReadEvent.number(1),
        ReadEvent.endRecord(),
        ReadEvent.slot(),
        ReadEvent.startAttribute("value"),
        ReadEvent.endAttribute(),
        ReadEvent.startBody(),
        ReadEvent.number(2),
        ReadEvent.endRecord(),
        ReadEvent.endRecord()
    );

    runTestOk("{@key {1}: @value {2}}", events);
  }
}