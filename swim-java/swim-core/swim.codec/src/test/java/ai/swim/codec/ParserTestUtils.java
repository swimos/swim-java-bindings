package ai.swim.codec;

import ai.swim.codec.input.Input;
import ai.swim.codec.result.ParseError;
import ai.swim.codec.result.ParseIncomplete;
import ai.swim.codec.result.Result;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ParserTestUtils {

  public static void runParserOk(Parser<String> p, String input, String output, String remaining) {
    final Result<String> result = p.parse(Input.string(input));
    assertEquals(output, result.getOutput());

    Input buf = result.getInput();
    StringBuilder sb = new StringBuilder();

    while (!buf.isDone()) {
      int head = buf.head();
      sb.append((char) head);
      buf = buf.next();
    }

    assertEquals(remaining, sb.toString());
  }

  public static <A> void runParserError(Parser<A> p, String input, Location location) {
    final Result<A> result = p.parse(Input.string(input));
    assertTrue(result.isError());
    assertEquals(((ParseError<A>) result).getLocation(), location);
  }

  public static <A> void runParserIncomplete(Parser<A> p, String input, int needed) {
    final Result<A> result = p.parse(Input.string(input));
    assertTrue(result.isIncomplete(), "Expected an incomplete input");
    assertEquals(((ParseIncomplete<A>) result).getNeeded(), needed);
  }


}
