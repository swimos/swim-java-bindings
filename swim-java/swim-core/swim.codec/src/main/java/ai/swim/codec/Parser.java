package ai.swim.codec;

import java.util.function.Function;
import ai.swim.codec.input.Input;

@FunctionalInterface
public interface Parser<I, O> {

  default Result<I, O> parse(Input<I> input) {
    return apply(input).getResult();
  }

  Cont<I, O> apply(Input<I> input);

  default <B> Parser<I, B> then(Function<O, Parser<I, B>> f) {
    return ParserExt.and(this, f);
  }



}
