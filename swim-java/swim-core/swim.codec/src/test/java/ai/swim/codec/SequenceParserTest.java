package ai.swim.codec;

import ai.swim.codec.character.CompleteCharacter;
import ai.swim.codec.models.Pair;
import ai.swim.codec.result.ParseError;
import ai.swim.codec.result.Result;
import ai.swim.codec.source.Source;
import org.junit.jupiter.api.Test;
import static ai.swim.codec.SequenceParser.delimited;
import static ai.swim.codec.SequenceParser.pair;
import static ai.swim.codec.SequenceParser.separatedPair;
import static ai.swim.codec.character.StreamingCharacter.tag;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SequenceParserTest {

  public static void pairOk(Parser<Pair<Source, Source>> p, String input, String o1, String o2, String remaining) {
    final Result<Pair<Source, Source>> result = p.parse(Source.string(input));

    assertTrue(result.isOk());
    assertEquals(o1, new String(result.getOutput().getOutput1().collect()));
    assertEquals(o2, new String(result.getOutput().getOutput2().collect()));

    assertEquals(remaining, new String(result.getInput().collect()));
  }

  public static void pairErr(Parser<Pair<Source, Source>> p, String input, String remainingInput, String cause) {
    final Result<Pair<Source, Source>> result = p.parse(Source.string(input));

    assertTrue(result.isError());
    assertEquals(remainingInput, new String(result.getInput().collect()));
    assertEquals(cause, ((ParseError<Pair<Source, Source>>) result).getCause());
  }

  @Test
  void separatedPairTest() {
    Parser<Pair<Source, Source>> parser = separatedPair(tag("abc"), tag("|"), tag("efg"));
    pairOk(parser, "abc|efg", "abc", "efg", "");
    pairOk(parser, "abc|efghij", "abc", "efg", "hij");
    pairErr(parser, "123", "123", "Expected a tag of: abc");
    pairErr(parser, "abc|123", "123", "Expected a tag of: efg");
  }

  @Test
  void pairTest() {
    Parser<Pair<Source, Source>> parser = pair(tag("abc"), tag("efg"));
    pairOk(parser, "abcefg", "abc", "efg", "");
    pairOk(parser, "abcefghij", "abc", "efg", "hij");
    pairErr(parser, "123", "123", "Expected a tag of: abc");
    pairErr(parser, "abc123", "123", "Expected a tag of: efg");
  }

  public static void delimitedOk(Parser<Source> p, String input, String expectedInput, String expectedOutput) {
    final Result<Source> result = p.parse(Source.string(input));

    assertTrue(result.isOk());
    assertEquals(expectedInput, new String(result.getInput().collect()));
    assertEquals(expectedOutput, new String(result.getOutput().collect()));
  }

  @Test
  void delimitedTest() {
    Parser<Source> parser = delimited(CompleteCharacter.tag("("), CompleteCharacter.tag("abc"), CompleteCharacter.tag(")"));
    delimitedOk(parser, "(abc)", "", "abc");
    delimitedOk(parser, "(abc)def", "def", "abc");

    assertTrue(parser.parse(Source.string("")).isIncomplete());
    assertTrue(parser.parse(Source.string("()")).isError());
  }

}