package ai.swim.codec;

import java.util.function.Function;
import ai.swim.codec.input.Input;
import ai.swim.codec.result.Result;

@FunctionalInterface
public interface Parser<O> {

  default Result<O> parse(Input input) {
    return apply(input).getResult();
  }

  Cont<O> apply(Input input);

  default <B> Parser<B> then(Function<O, Parser<B>> f) {
    return ParserExt.and(this, f);
  }


}
