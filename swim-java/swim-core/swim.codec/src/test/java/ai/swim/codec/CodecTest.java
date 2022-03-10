package ai.swim.codec;

import org.junit.jupiter.api.Test;
import static ai.swim.codec.ParserExt.eq;
import static ai.swim.codec.ParserExt.preceded;
import static ai.swim.codec.ParserExt.then;
import static ai.swim.codec.ParserTestUtils.runParserError;
import static ai.swim.codec.ParserTestUtils.runParserIncomplete;
import static ai.swim.codec.ParserTestUtils.runParserOk;
import static ai.swim.codec.character.StringParser.alpha1;
import static ai.swim.codec.character.StringParser.eqChar;

public class CodecTest {

  @Test
  public void simpleCombinator() {
    final Parser<Character, String> p = eq('a').then(a -> eq('b').then(b -> then("" + a + b)));

    runParserIncomplete(p, "a", 1);
    runParserError(p, "b", Location.of(0, 0));
    runParserOk(p, "ab", "ab", "");
  }

  @Test
  void complexCombinator() {
    Parser<Character, String> parser = preceded(eqChar('@'), alpha1())
        .then(c -> eqChar('('))
        .then(c -> alpha1())
        .then(c -> eqChar(')'));

    runParserOk(parser, "@event(something)", ")", "");
  }

}