package ai.swim.codec.character;

import ai.swim.codec.Location;
import ai.swim.codec.Parser;
import ai.swim.codec.result.Result;
import ai.swim.codec.source.Source;
import ai.swim.codec.source.StringSource;
import org.junit.jupiter.api.Test;
import static ai.swim.codec.ParserTestUtils.runParserError;
import static ai.swim.codec.ParserTestUtils.runParserIncomplete;
import static ai.swim.codec.ParserTestUtils.runParserOk;
import static ai.swim.codec.character.StreamingCharacter.stringLiteral;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StreamingCharacterTest {

  @Test
  void eqChar() {
    runParserOk(StreamingCharacter.eqChar('a'), "aa", Source.string("a"), Source.string("a"));
    runParserError(StreamingCharacter.eqChar('b'), "aa", Location.of(1, 1));
  }

  @Test
  void alpha0() {
    runParserOk(StreamingCharacter.alpha0(), "aa", Source.string("aa"), Source.string(""));
    runParserOk(StreamingCharacter.alpha0(), "aab", Source.string("aab"), Source.string(""));
    runParserOk(StreamingCharacter.alpha0(), "aab1", Source.string("aab"), Source.string("1"));
    runParserOk(StreamingCharacter.alpha0(), "!!", Source.string(""), Source.string("!!"));
    runParserIncomplete(StreamingCharacter.alpha0(), "", 1);
  }

  @Test
  void alpha1() {
    runParserOk(StreamingCharacter.alpha1(), "aa", Source.string("aa"), Source.string(""));
    runParserOk(StreamingCharacter.alpha1(), "aab", Source.string("aab"), Source.string(""));
    runParserOk(StreamingCharacter.alpha1(), "aab1", Source.string("aab"), Source.string("1"));
    runParserError(StreamingCharacter.alpha1(), "!!", Location.of(1, 1));
  }

  @Test
  void alphanumeric0() {
    runParserOk(StreamingCharacter.alphanumeric0(), "aa", Source.string("aa"), Source.string(""));
    runParserOk(StreamingCharacter.alphanumeric0(), "aab", Source.string("aab"), Source.string(""));
    runParserOk(StreamingCharacter.alphanumeric0(), "aab1!", Source.string("aab1"), Source.string("!"));
    runParserOk(StreamingCharacter.alphanumeric0(), "!!", Source.string(""), Source.string("!!"));
    runParserIncomplete(StreamingCharacter.alphanumeric0(), "", 1);
  }

  @Test
  void alphanumeric1() {
    runParserOk(StreamingCharacter.alphanumeric1(), "aa123", Source.string("aa123"), Source.string(""));
    runParserOk(StreamingCharacter.alphanumeric1(), "aab123", Source.string("aab123"), Source.string(""));
    runParserOk(StreamingCharacter.alphanumeric1(), "aab1!", Source.string("aab1"), Source.string("!"));
    runParserError(StreamingCharacter.alphanumeric1(), "!!", Location.of(1, 1));
    runParserIncomplete(StreamingCharacter.alphanumeric1(), "", 1);
  }

  @Test
  void digit0() {
    runParserOk(StreamingCharacter.digit0(), "123", Source.string("123"), Source.string(""));
    runParserOk(StreamingCharacter.digit0(), "123!", Source.string("123"), Source.string("!"));
    runParserOk(StreamingCharacter.digit0(), "!!", Source.string(""), Source.string("!!"));
    runParserIncomplete(StreamingCharacter.digit0(), "", 1);
  }

  @Test
  void digit1() {
    runParserOk(StreamingCharacter.digit1(), "123", Source.string("123"), Source.string(""));
    runParserOk(StreamingCharacter.digit1(), "123!", Source.string("123"), Source.string("!"));
    runParserError(StreamingCharacter.digit1(), "!!", Location.of(1, 1));
    runParserIncomplete(StreamingCharacter.digit1(), "", 1);
  }

  @Test
  void tag() {
    runParserOk(StreamingCharacter.tag("abc"), "abcdefg", Source.string("abc"), Source.string("defg"));
    runParserError(StreamingCharacter.tag("abc"), "def", Location.of(1, 1));
    runParserIncomplete(StreamingCharacter.tag("abc"), "a", 2);
  }

  @Test
  void satisfy() {
    Parser<Character> p = StreamingCharacter.satisfy(c -> c == 'a' || c == 'b');
    Result<Character> result = p.parse(Source.string("abc"));
    assertTrue(result.isOk());
    assertEquals(result.getOutput(), 'a');
    assertEquals(result.getInput(), new StringSource("abc", 1, 2, 1, 1));

    runParserError(StreamingCharacter.satisfy(c -> c == 'a' || c == 'b'), "cd", Location.of(1, 1));
    runParserIncomplete(StreamingCharacter.satisfy(c -> c == 'a' || c == 'b'), "", 1);
  }

  @Test
  void tagNoCase() {
    runParserOk(StreamingCharacter.tagNoCase("abc"), "abcdefg", Source.string("abc"), Source.string("defg"));
    runParserOk(StreamingCharacter.tagNoCase("ABC"), "ABCDEFG", Source.string("ABC"), Source.string("DEFG"));
    runParserOk(StreamingCharacter.tagNoCase("ABCdef"), "ABCDEFG", Source.string("ABCDEF"), Source.string("G"));

    runParserError(StreamingCharacter.tagNoCase("abc"), "def", Location.of(1, 1));
    runParserIncomplete(StreamingCharacter.tagNoCase("abc"), "a", 2);
  }

  @Test
  void testStringLiteral() {
    runParserOk(stringLiteral(), "\"\"", Source.string(""), Source.string(""));
    runParserOk(stringLiteral(), "\"multiple words\"", Source.string("multiple words"), Source.string(""));
    runParserOk(stringLiteral(), "\"multiple\nwords\"", Source.string("multiple\nwords"), Source.string(""));
  }

}