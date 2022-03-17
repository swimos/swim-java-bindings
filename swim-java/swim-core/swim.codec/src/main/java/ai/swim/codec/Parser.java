package ai.swim.codec;

import java.util.function.Function;
import ai.swim.codec.result.Result;
import ai.swim.codec.source.Source;

@FunctionalInterface
public interface Parser<O> {

  String INCOMPLETE_INPUT = "Incomplete input";

  private static <O> Parser<O> inputOpt(Parser<O> parser, Function<Source, Result<O>> onComplete) {
    return input -> {
      if (input.complete()) {
        return Cont.none(onComplete.apply(input));
      } else {
        return parser.apply(input);
      }
    };
  }

  static <O> Parser<O> streaming(Parser<O> parser, int expected) {
    return inputOpt(parser, i -> Result.incomplete(i, expected));
  }

  static <O> Parser<O> complete(Parser<O> parser) {
    return inputOpt(parser, i -> Result.error(i, Parser.INCOMPLETE_INPUT));
  }

  static <O> Parser<O> complete(Parser<O> parser, String cause) {
    return inputOpt(parser, i -> Result.error(i, cause));
  }

  default Result<O> parse(Source source) {
    return apply(source).getResult();
  }

  Cont<O> apply(Source source);

  default <B> Parser<B> then(Function<O, Parser<B>> f) {
    return ParserExt.and(this, f);
  }

}
