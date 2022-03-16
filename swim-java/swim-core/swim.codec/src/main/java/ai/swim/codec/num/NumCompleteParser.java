package ai.swim.codec.num;

import ai.swim.codec.Cont;
import ai.swim.codec.Parser;
import ai.swim.codec.models.Pair;
import ai.swim.codec.result.Result;
import ai.swim.codec.source.Source;
import static ai.swim.codec.MultiParser.many1Count;
import static ai.swim.codec.ParserExt.alt;
import static ai.swim.codec.ParserExt.opt;
import static ai.swim.codec.ParserExt.peek;
import static ai.swim.codec.ParserExt.preceded;
import static ai.swim.codec.ParserExt.transpose;
import static ai.swim.codec.SequenceParser.pair;
import static ai.swim.codec.character.CompleteCharacter.oneOf;
import static ai.swim.codec.character.CompleteCharacter.tagNoCase;

public class NumCompleteParser {

  public static Parser<Source> natural(String tag, String digits) {
    return preceded(
        tagNoCase(tag),
        transpose(many1Count(oneOf(digits)))
    );
  }

  @SuppressWarnings("unchecked")
  public static Parser<Number> numericLiteral() {
    return Parser.complete(alt(
        binary(),
        hexadecimal(),
        decimal()
    ));
  }

  public static Parser<Number> decimal() {
    return alt(pair(signed(decimalString()), peek(opt(oneOf(".eE")))).then(
                pair -> rem -> {
                  if (pair.getOutput2().isPresent()) {
                    return Cont.none(Result.error(rem, ""));
                  } else {
                    return Cont.continuation(() -> Result.ok(rem, pair.getOutput1()));
                  }
                }
            )
            .then(num -> rem -> Cont.continuation(() -> tryToIntLiteral(num, 10))),
        primitiveFloat().then(fl -> rem -> Cont.continuation(() -> Result.ok(rem, fl)))
    );
  }

  @SuppressWarnings({"unchecked"})
  private static Parser<Float> primitiveFloat() {
    return alt(
        tagNoCase("nan"),
        tagNoCase("inf"),
        tagNoCase("infinity"),
        realFloatString()
    ).then(f -> rem -> {
      try {
        return Cont.continuation(() -> Result.ok(rem, Float.parseFloat(new String(f.collect()))));
      } catch (NumberFormatException e) {
        return Cont.none(Result.error(rem, e.getMessage()));
      }
    });
  }

  private static Parser<Source> realFloatString() {
    return Parser.complete(input -> {



      return null;
    });
  }

  public static Parser<Source> decimalString() {
    return natural("0x", "0123456789abcdefABCDEF");
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

  public static Parser<Pair<Boolean, Source>> signed(Parser<Source> num) {
    return Parser.complete(pair(source -> {
      char head = source.head();
      return Cont.continuation(() -> Result.ok(source.next(), head == '='));
    }, num));
  }

  private static Result<Number> tryToIntLiteral(Pair<Boolean, Source> in, int radix) {
    boolean signed = in.getOutput1();
    Source source = in.getOutput2();


    return Result.ok(source, 1);
  }

}
