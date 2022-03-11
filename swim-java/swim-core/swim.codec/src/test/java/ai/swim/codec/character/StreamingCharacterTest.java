package ai.swim.codec.character;

import ai.swim.codec.Location;
import org.junit.jupiter.api.Test;
import static ai.swim.codec.ParserTestUtils.runParserError;
import static ai.swim.codec.ParserTestUtils.runParserIncomplete;
import static ai.swim.codec.ParserTestUtils.runParserOk;

public class StreamingCharacterTest {

  @Test
  void eqChar() {
    runParserOk(StreamingCharacter.eqChar('a'), "aa", "a", "a");
    runParserError(StreamingCharacter.eqChar('b'), "aa", Location.of(1, 1));
  }

  @Test
  void alpha0() {
    runParserOk(StreamingCharacter.alpha0(), "aa", "aa", "");
    runParserOk(StreamingCharacter.alpha0(), "aab", "aab", "");
    runParserOk(StreamingCharacter.alpha0(), "aab1", "aab", "1");
    runParserOk(StreamingCharacter.alpha0(), "!!", "", "!!");
    runParserIncomplete(StreamingCharacter.alpha0(), "", 1);
  }

  @Test
  void alpha1() {
    runParserOk(StreamingCharacter.alpha1(), "aa", "aa", "");
    runParserOk(StreamingCharacter.alpha1(), "aab", "aab", "");
    runParserOk(StreamingCharacter.alpha1(), "aab1", "aab", "1");
    runParserError(StreamingCharacter.alpha1(), "!!", Location.of(1, 1));
  }

  @Test
  void alphanumeric0() {
    runParserOk(StreamingCharacter.alphanumeric0(), "aa", "aa", "");
    runParserOk(StreamingCharacter.alphanumeric0(), "aab", "aab", "");
    runParserOk(StreamingCharacter.alphanumeric0(), "aab1!", "aab1", "!");
    runParserOk(StreamingCharacter.alphanumeric0(), "!!", "", "!!");
    runParserIncomplete(StreamingCharacter.alphanumeric0(), "", 1);
  }

  @Test
  void alphanumeric1() {
    runParserOk(StreamingCharacter.alphanumeric1(), "aa123", "aa123", "");
    runParserOk(StreamingCharacter.alphanumeric1(), "aab123", "aab123", "");
    runParserOk(StreamingCharacter.alphanumeric1(), "aab1!", "aab1", "!");
    runParserError(StreamingCharacter.alphanumeric1(), "!!", Location.of(1, 1));
    runParserIncomplete(StreamingCharacter.alphanumeric1(), "", 1);
  }

  @Test
  void digit0() {
    runParserOk(StreamingCharacter.digit0(), "123", "123", "");
    runParserOk(StreamingCharacter.digit0(), "123!", "123", "!");
    runParserOk(StreamingCharacter.digit0(), "!!", "", "!!");
    runParserIncomplete(StreamingCharacter.digit0(), "", 1);
  }

  @Test
  void digit1() {
    runParserOk(StreamingCharacter.digit1(), "123", "123", "");
    runParserOk(StreamingCharacter.digit1(), "123!", "123", "!");
    runParserError(StreamingCharacter.digit1(), "!!", Location.of(1, 1));
    runParserIncomplete(StreamingCharacter.digit1(), "", 1);
  }

  @Test
  void tag() {
    runParserOk(StreamingCharacter.tag("abc"), "abcdefg", "abc", "defg");
    runParserError(StreamingCharacter.tag("abc"), "def", Location.of(1,1));
    runParserIncomplete(StreamingCharacter.tag("abc"), "a", 2);
  }

  @Test
  void satisfy() {
    runParserOk(StreamingCharacter.satisfy(c -> c == 'a' || c == 'b'), "abc", "a", "bc");
    runParserError(StreamingCharacter.satisfy(c -> c == 'a' || c == 'b'), "cd", Location.of(1, 1));
    runParserIncomplete(StreamingCharacter.satisfy(c -> c == 'a' || c == 'b'), "", 1);
  }

}