package ai.swim.codec;

import java.util.function.Function;
import ai.swim.codec.result.Result;
import ai.swim.codec.source.Source;

@FunctionalInterface
public interface Parser<O> {

  default Result<O> parse(Source source) {
    return apply(source).getResult();
  }

  Cont<O> apply(Source source);

  default <B> Parser<B> then(Function<O, Parser<B>> f) {
    return ParserExt.and(this, f);
  }

}
