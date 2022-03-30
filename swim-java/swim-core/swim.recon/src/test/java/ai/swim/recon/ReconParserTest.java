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
//    testIncrementalOk(string, expected);
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
    runTestOk(" { 1 , two , 3 } ", events);
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

    runTestOk("{1\ntwo\n3}",events);
    runTestOk("{\n\t1\n\ttwo\n\t3\n}",events);
  }
}