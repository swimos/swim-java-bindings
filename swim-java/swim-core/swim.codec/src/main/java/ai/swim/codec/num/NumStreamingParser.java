package ai.swim.codec.num;

import java.text.NumberFormat;
import java.util.function.BiFunction;
import ai.swim.codec.Cont;
import ai.swim.codec.Parser;
import ai.swim.codec.SequenceParser;
import ai.swim.codec.result.Result;
import ai.swim.codec.source.Source;
import static ai.swim.codec.MultiParser.many1Count;
import static ai.swim.codec.ParserExt.alt;
import static ai.swim.codec.ParserExt.preceded;
import static ai.swim.codec.ParserExt.transpose;
import static ai.swim.codec.SequenceParser.pair;
import static ai.swim.codec.character.StreamingCharacter.oneOf;
import static ai.swim.codec.character.StreamingCharacter.tagNoCase;

public class NumStreamingParser {

  public static Parser<Source> natural(String tag, String digits) {
    return preceded(
        tagNoCase(tag),
        transpose(many1Count(oneOf(digits)))
    );
  }

  public static Parser<Source> binaryString() {
    return natural("0b", "01");
  }

  public static Parser<Number> binary() {
    return signed(hexadecimalString()).then(num -> rem -> Cont.continuation(() -> tryToIntLiteral(num, 2)));
  }

  public static Parser<Source> hexadecimalString() {
    return natural("0x", "0123456789abcdefABCDEF");
  }

  public static Parser<Number> hexadecimal() {
    return signed(hexadecimalString()).then(num -> rem -> Cont.continuation(() -> tryToIntLiteral(num, 16)));
  }

  public static Parser<SequenceParser.Pair<Boolean, Source>> signed(Parser<Source> num) {
    return pair(source -> {
      if (source.complete()) {
        return Cont.none(Result.incomplete(source, 1));
      } else {
        char head = source.head();
        return Cont.continuation(() -> Result.ok(source.next(), head == '-'));
      }
    }, num);
  }

  private static Result<Number> tryToIntLiteral(SequenceParser.Pair<Boolean, Source> in, int radix) {
    boolean signed = in.getOutput1();
    Source source = in.getOutput2();

    return Result.ok(source, 1);
  }

}
