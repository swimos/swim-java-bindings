package ai.swim.codec;

import ai.swim.codec.input.Input;
import ai.swim.codec.result.ParseError;
import ai.swim.codec.result.Result;
import org.junit.jupiter.api.Test;
import static ai.swim.codec.character.CompleteCharacter.tag;
import static ai.swim.codec.MultiParser.many0Count;
import static ai.swim.codec.MultiParser.many1Count;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiParserTest {

  public static void manyOk(Parser<Integer> p, String input, int count, String remaining) {
    final Result<Integer> result = p.parse(Input.string(input));

    assertTrue(result.isOk());
    assertEquals(count, result.getOutput());
    assertEquals(remaining,new String(result.getInput().collect()));
  }

  public static <A> void manyError(Parser<Integer> p, String input, Location location) {
    final Result<Integer> result = p.parse(Input.string(input));
    assertTrue(result.isError());
    assertEquals(((ParseError<A>) result).getLocation(), location);
  }

  @Test
  void many0CountTest() {
    Parser<Integer> parser = many0Count(tag("abc"));
    manyOk(parser, "abcabcabc",  3, "");
    manyOk(parser, "abc123",  1, "123");
    manyOk(parser, "123123",  0, "123123");
  }

  @Test
  void many1CountTest() {
    Parser<Integer> parser = many1Count(tag("abc"));
    manyOk(parser, "abcabc",  2, "");
    manyOk(parser, "abc123",  1, "123");
    manyError(parser, "123", Location.of(1,1));
  }

}