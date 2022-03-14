package ai.swim.codec.character;

import ai.swim.codec.Location;
import org.junit.jupiter.api.Test;
import static ai.swim.codec.ParserTestUtils.runParserError;
import static ai.swim.codec.ParserTestUtils.runParserOk;

class CompleteCharacterTest {

  @Test
  void tag() {
    runParserOk(CompleteCharacter.tag("abc"), "abcdefg", "abc", "defg");
    runParserError(CompleteCharacter.tag("abc"), "a", Location.of(1, 1));
  }

}