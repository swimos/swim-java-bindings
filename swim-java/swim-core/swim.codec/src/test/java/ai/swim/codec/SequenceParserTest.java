package ai.swim.codec;

import ai.swim.codec.input.Input;
import ai.swim.codec.result.ParseError;
import ai.swim.codec.result.Result;
import org.junit.jupiter.api.Test;
import static ai.swim.codec.character.StreamingCharacter.tag;
import static ai.swim.codec.SequenceParser.pair;
import static ai.swim.codec.SequenceParser.separatedPair;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SequenceParserTest {

  public static void pairOk(Parser<SequenceParser.Pair<Input, Input>> p, String input, String o1, String o2, String remaining) {
    final Result<SequenceParser.Pair<Input, Input>> result = p.parse(Input.string(input));

    assertTrue(result.isOk());
    assertEquals(o1, new String(result.getOutput().getOutput1().collect()));
    assertEquals(o2, new String(result.getOutput().getOutput2().collect()));

    assertEquals(remaining, new String(result.getInput().collect()));
  }

  public static void pairErr(Parser<SequenceParser.Pair<Input, Input>> p, String input, String remainingInput, String cause) {
    final Result<SequenceParser.Pair<Input, Input>> result = p.parse(Input.string(input));

    assertTrue(result.isError());
    assertEquals(remainingInput, new String(result.getInput().collect()));
    assertEquals(cause, ((ParseError<SequenceParser.Pair<Input,Input>>) result).getCause());
  }

  @Test
  void separatedPairTest() {
    Parser<SequenceParser.Pair<Input, Input>> parser = separatedPair(tag("abc"), tag("|"), tag("efg"));
    pairOk(parser, "abc|efg", "abc", "efg", "");
    pairOk(parser, "abc|efghij", "abc", "efg", "hij");
    pairErr(parser, "123", "123", "Expected a tag of: abc");
    pairErr(parser, "abc|123", "123", "Expected a tag of: efg");
  }

  @Test
  void pairTest() {
    Parser<SequenceParser.Pair<Input, Input>> parser = pair(tag("abc"), tag("efg"));
    pairOk(parser, "abcefg", "abc", "efg", "");
    pairOk(parser, "abcefghij", "abc", "efg", "hij");
    pairErr(parser, "123", "123", "Expected a tag of: abc");
    pairErr(parser, "abc123", "123", "Expected a tag of: efg");
  }

}