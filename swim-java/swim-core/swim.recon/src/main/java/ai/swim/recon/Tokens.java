package ai.swim.recon;


import java.util.Arrays;
import ai.swim.codec.Parser;
import ai.swim.codec.result.Result;
import ai.swim.codec.source.Source;
import ai.swim.codec.source.StringSource;
import ai.swim.recon.utils.Either;
import static ai.swim.codec.Cont.continuation;
import static ai.swim.codec.MultiParser.many0Count;
import static ai.swim.codec.ParserExt.transpose;
import static ai.swim.codec.SequenceParser.pair;
import static ai.swim.codec.character.StreamingCharacter.satisfy;

public class Tokens {


  static boolean isIdentifierStart(int c) {
    return c >= 'A' && c <= 'Z'
        || c == '_'
        || c >= 'a' && c <= 'z'
        || c == 0xb7
        || c >= 0xc0 && c <= 0xd6
        || c >= 0xd8 && c <= 0xf6
        || c >= 0xf8 && c <= 0x37d
        || c >= 0x37f && c <= 0x1fff
        || c >= 0x200c && c <= 0x200d
        || c >= 0x2070 && c <= 0x218f
        || c >= 0x2c00 && c <= 0x2fef
        || c >= 0x3001 && c <= 0xd7ff
        || c >= 0xf900 && c <= 0xfdcf
        || c >= 0xfdf0 && c <= 0xfffd
        || c >= 0x10000 && c <= 0xeffff;
  }


  static boolean isIdentifierChar(int c) {
    return isIdentifierStart(c)
        || c >= '-'
        || (char) c >= '0' && (char) c <= '9';
  }

  public static Parser<Source> identifier() {
    return transpose(pair(
        satisfy(Tokens::isIdentifierStart),
        many0Count(satisfy(Tokens::isIdentifierChar))
    ));
  }

  public static Parser<Either<String, Boolean>> identifierOrBoolean() {
    return identifier().then(in -> rem -> {
      int[] codePoints = in.collect();

      if (Arrays.equals("true".chars().toArray(), codePoints)) {
        return continuation(() -> Result.ok(rem, Either.right(true)));
      } else if (Arrays.equals("false".chars().toArray(), codePoints)) {
        return continuation(() -> Result.ok(rem, Either.right(false)));
      } else {
        return continuation(() -> Result.ok(rem, Either.left(StringSource.codePointsToString(codePoints))));
      }
    });
  }

}
