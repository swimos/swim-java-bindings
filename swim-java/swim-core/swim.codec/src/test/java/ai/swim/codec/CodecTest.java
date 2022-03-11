package ai.swim.codec;

import ai.swim.codec.input.Input;
import org.junit.jupiter.api.Test;
import static ai.swim.codec.ParserExt.eq;
import static ai.swim.codec.ParserExt.preceded;
import static ai.swim.codec.ParserExt.then;
import static ai.swim.codec.ParserTestUtils.runParserError;
import static ai.swim.codec.ParserTestUtils.runParserIncomplete;
import static ai.swim.codec.ParserTestUtils.runParserOk;
import static ai.swim.codec.character.StreamingCharacter.alpha1;
import static ai.swim.codec.character.StreamingCharacter.eqChar;

public class CodecTest {

  @Test
  public void simpleCombinator() {
    final Parser<Input> p = eq("a").then(a -> eq("b").then(b -> then(Input.string("" + a + b))));

    runParserIncomplete(p, "a", 1);
    runParserError(p, "b", Location.of(1, 1));
    runParserOk(p, "ab", "ab", "");
  }

  @Test
  void complexCombinator() {
    Parser<Input> parser = preceded(eqChar('@'), alpha1())
        .then(c -> eqChar('('))
        .then(c -> alpha1())
        .then(c -> eqChar(')'));

    runParserOk(parser, "@event(something)", ")", "");
  }

  static boolean isIdentStartChar(int c) {
    return c >= 'A' && c <= 'Z'
        || c == '_'
        || c >= 'a' && c <= 'z'
        || c >= 0xc0 && c <= 0xd6
        || c >= 0xd8 && c <= 0xf6
        || c >= 0xf8 && c <= 0x2ff
        || c >= 0x370 && c <= 0x37d
        || c >= 0x37f && c <= 0x1fff
        || c >= 0x200c && c <= 0x200d
        || c >= 0x2070 && c <= 0x218f
        || c >= 0x2c00 && c <= 0x2fef
        || c >= 0x3001 && c <= 0xd7ff
        || c >= 0xf900 && c <= 0xfdcf
        || c >= 0xfdf0 && c <= 0xfffd
        || c >= 0x10000 && c <= 0xeffff;
  }


  static boolean isIdentChar(int c) {
    return c == '-'
        || c >= '0' && c <= '9'
        || c >= 'A' && c <= 'Z'
        || c == '_'
        || c >= 'a' && c <= 'z'
        || c == 0xb7
        || c >= 0xc0 && c <= 0xd6
        || c >= 0xd8 && c <= 0xf6
        || c >= 0xf8 && c <= 0x37d
        || c >= 0x37f && c <= 0x1fff
        || c >= 0x200c && c <= 0x200d
        || c >= 0x203f && c <= 0x2040
        || c >= 0x2070 && c <= 0x218f
        || c >= 0x2c00 && c <= 0x2fef
        || c >= 0x3001 && c <= 0xd7ff
        || c >= 0xf900 && c <= 0xfdcf
        || c >= 0xfdf0 && c <= 0xfffd
        || c >= 0x10000 && c <= 0xeffff;
  }

  @Test
  void identifier() {
//    Parser<String> recognize = recognize(
//        pair(
//            satisfy(CodecTest::isIdentStartChar),
//            many0Count(satisfy(CodecTest::isIdentChar))
//        )
//    );
  }

}