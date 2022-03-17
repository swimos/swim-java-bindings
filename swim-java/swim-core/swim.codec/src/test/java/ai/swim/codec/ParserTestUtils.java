package ai.swim.codec;

import ai.swim.codec.result.ParseError;
import ai.swim.codec.result.ParseIncomplete;
import ai.swim.codec.result.Result;
import ai.swim.codec.source.Source;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ParserTestUtils {

  public static <O extends Source> void runParserOk(Parser<O> p, String input, O output, O remaining) {
    final Result<O> result = p.parse(Source.string(input));
    assertTrue(result.isOk());

    assertTrue(remaining.dataEquals(result.getInput()));
    assertEquals(output, result.getOutput());
  }

  public static <A> void runParserError(Parser<A> p, String input, Location location) {
    final Result<A> result = p.parse(Source.string(input));
    assertTrue(result.isError());
    assertEquals(((ParseError<A>) result).getLocation(), location);
  }

  public static <A> void runParserIncomplete(Parser<A> p, String input, int needed) {
    final Result<A> result = p.parse(Source.string(input));
    assertTrue(result.isIncomplete(), "Expected an incomplete input");
    assertEquals(((ParseIncomplete<A>) result).getNeeded(), needed);
  }


}
