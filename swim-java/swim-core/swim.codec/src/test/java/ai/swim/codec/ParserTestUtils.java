package ai.swim.codec;

import ai.swim.codec.input.Input;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ParserTestUtils {

  public static void runParserOk(Parser<Character, String> p, String input, String output, String remaining) {
    final Result<Character, String> result = p.parse(Input.string(input));
    assertEquals(output, result.getOutput());

    Input<Character> buf = result.getInput();
    StringBuilder sb = new StringBuilder();

    while (!buf.isDone()) {
      int head = buf.head();
      sb.append((char) head);
      buf = buf.next();
    }

    assertEquals(remaining, sb.toString());
  }

  public static <A> void runParserError(Parser<Character, A> p, String input, Location location) {
    final Result<Character, A> result = p.parse(Input.string(input));
    assertTrue(result.isError());
    assertEquals(((ParseError<Character, A>) result).getLocation(), location);
  }

  public static <A> void runParserIncomplete(Parser<Character, A> p, String input, int needed) {
    final Result<Character, A> result = p.parse(Input.string(input));
    assertTrue(result.isIncomplete(), "Expected an incomplete input");
    assertEquals(((ParseIncomplete<Character, A>) result).getNeeded(), needed);
  }


}
