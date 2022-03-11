package ai.swim.codec.character;

import ai.swim.codec.Location;
import org.junit.jupiter.api.Test;
import static ai.swim.codec.ParserTestUtils.runParserError;
import static ai.swim.codec.ParserTestUtils.runParserIncomplete;
import static ai.swim.codec.ParserTestUtils.runParserOk;

public class StringParserTest {

  @Test
  void eqChar() {
    runParserOk(StringParser.eqChar('a'), "aa", "a", "a");
    runParserError(StringParser.eqChar('b'), "aa", Location.of(1, 1));
  }

  @Test
  void alpha0() {
    runParserOk(StringParser.alpha0(), "aa", "aa", "");
    runParserOk(StringParser.alpha0(), "aab", "aab", "");
    runParserOk(StringParser.alpha0(), "aab1", "aab", "1");
    runParserOk(StringParser.alpha0(), "!!", "", "!!");
    runParserIncomplete(StringParser.alpha0(), "", 1);
  }

  @Test
  void alpha1() {
    runParserOk(StringParser.alpha1(), "aa", "aa", "");
    runParserOk(StringParser.alpha1(), "aab", "aab", "");
    runParserOk(StringParser.alpha1(), "aab1", "aab", "1");
    runParserError(StringParser.alpha1(), "!!", Location.of(1, 1));
  }

  @Test
  void alphanumeric0() {
    runParserOk(StringParser.alphanumeric0(), "aa", "aa", "");
    runParserOk(StringParser.alphanumeric0(), "aab", "aab", "");
    runParserOk(StringParser.alphanumeric0(), "aab1!", "aab1", "!");
    runParserOk(StringParser.alphanumeric0(), "!!", "", "!!");
    runParserIncomplete(StringParser.alphanumeric0(), "", 1);
  }

  @Test
  void alphanumeric1() {
    runParserOk(StringParser.alphanumeric1(), "aa123", "aa123", "");
    runParserOk(StringParser.alphanumeric1(), "aab123", "aab123", "");
    runParserOk(StringParser.alphanumeric1(), "aab1!", "aab1", "!");
    runParserError(StringParser.alphanumeric1(), "!!", Location.of(1, 1));
    runParserIncomplete(StringParser.alphanumeric1(), "", 1);
  }

  @Test
  void digit0() {
    runParserOk(StringParser.digit0(), "123", "123", "");
    runParserOk(StringParser.digit0(), "123!", "123", "!");
    runParserOk(StringParser.digit0(), "!!", "", "!!");
    runParserIncomplete(StringParser.digit0(), "", 1);
  }

  @Test
  void digit1() {
    runParserOk(StringParser.digit1(), "123", "123", "");
    runParserOk(StringParser.digit1(), "123!", "123", "!");
    runParserError(StringParser.digit1(), "!!", Location.of(1, 1));
    runParserIncomplete(StringParser.digit1(), "", 1);
  }

  @Test
  void tag() {
    runParserOk(StringParser.tag("abc"), "abcdefg", "abc", "defg");
  }

  @Test
  void satisfy() {
    runParserOk(StringParser.satisfy(c -> c == 'a' || c == 'b'), "abc", "a", "bc");
    runParserError(StringParser.satisfy(c -> c == 'a' || c == 'b'), "cd", Location.of(1, 1));
    runParserIncomplete(StringParser.satisfy(c -> c == 'a' || c == 'b'), "", 1);
  }

}