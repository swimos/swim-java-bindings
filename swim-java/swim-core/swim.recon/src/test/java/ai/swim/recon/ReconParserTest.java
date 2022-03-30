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
//    testCompleteOk(string, expected);
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
        System.out.println("ReadEvent: " + parseResult.bind());
        actual.add(parseResult.bind());
      }

      String s = String.valueOf(string.charAt(i));
      boolean isPartial = i != string.length() - 1;

      System.out.println("Feeding: " + s + ", is partial: "+ isPartial);

      parser = parser.feed(Input.string(s).isPartial(isPartial));
    }

    // A sufficiently large enough loop to drain any pending events but not cause an infinite loop
    for (int i = 0; i < 1000; i++) {
      ParseResult<ReadEvent> parseResult = parser.next();
      if (parseResult.isError()) {
        fail(((ResultError<?>) parseResult).getCause());
      }

      assertTrue(parseResult.isOk());
      actual.add(parseResult.bind());

      if (parser.isDone()) {
        break;
      }
    }

    assertTrue(parser.isDone());
    assertEquals(expected, actual);
  }

  void testCompleteOk(String input, List<ReadEvent> expected) {
    ReconParser parser = new ReconParser(Input.string(input));

    for (ReadEvent event : expected) {
      System.out.println(event);
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
//    runTestOk("@tag(a:1,b:abc,c:\"string\")", List.of(
//        ReadEvent.startAttribute("tag"),
//        ReadEvent.text("a"),
//        ReadEvent.slot(),
//        ReadEvent.number(1),
//        ReadEvent.text("b"),
//        ReadEvent.slot(),
//        ReadEvent.text("abc"),
//        ReadEvent.text("c"),
//        ReadEvent.slot(),
//        ReadEvent.text("string"),
//        ReadEvent.endAttribute(),
//        ReadEvent.startBody(),
//        ReadEvent.endRecord()
//    ));

    runTestOk("\"string\"", List.of(ReadEvent.text("string")));

//    runTestOk("@tag(a:\"abc\")",
//        List.of(
//            ReadEvent.startAttribute("tag"),
//            ReadEvent.text("a"),
//            ReadEvent.slot(),
//            ReadEvent.text("abc"),
//            ReadEvent.endAttribute(),
//            ReadEvent.startBody(),
//            ReadEvent.endRecord()
//        )
//    );
  }
}