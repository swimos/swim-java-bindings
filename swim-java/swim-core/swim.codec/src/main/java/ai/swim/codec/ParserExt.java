package ai.swim.codec;

import java.util.function.Function;
import java.util.function.Predicate;
import ai.swim.codec.input.Input;
import static ai.swim.codec.Cont.continuation;
import static ai.swim.codec.Cont.none;

public class ParserExt {

  public static <I, O> Parser<I, O> then(O x) {
    return input -> none(Result.ok(input, x));
  }

  public static <I> Parser<I, I> eq(I value) {
    return pred(value::equals);
  }

  public static <I> Parser<I, I> pred(Predicate<I> predicate) {
    return input -> {
      if (input.complete()) {
        return none(Result.incomplete(input, 1));
      } else {
        final I head = input.head();
        if (predicate.test(head)) {
          final Input<I> newInput = input.next();
          return continuation(() -> Result.ok(newInput, head));
        } else {
          return none(Result.error(input, null));
        }
      }
    };
  }

  public static <I, O> Parser<I, O> preceded(Parser<I, O> first, Parser<I, O> second) {
    return input -> {
      final Cont<I, ? extends O> firstCont = first.apply(input);
      if (firstCont.isContinuation()) {
        return second.apply(input.next());
      } else {
        return none(Result.error(input, null));
      }
    };
  }

  public static <I, O, B> Parser<I, B> and(Parser<I, ? extends O> first, Function<O, Parser<I, B>> second) {
    return input -> {
      final Cont<I, ? extends O> fistCont = first.apply(input);
      if (fistCont.isContinuation()) {
        return continuation(() -> fistCont.getResult().match(
                ok -> {
                  return second.apply(ok.getOutput()).apply(ok.getInput()).getResult();//.match(
//                  ok2 -> ok.merge(ok2),
//                  Result::cast,
//                  Result::cast)
                },
                Result::cast,
                Result::cast)
        );
      } else {
        return fistCont.getResult().match(
            ok -> {
              final Cont<I, B> secondCont = second.apply(ok.getOutput()).apply(ok.getInput());
              if (secondCont.isContinuation()) {
                return secondCont;
              } else {
                return none(secondCont.getResult());
              }
            },
            error -> none(error.cast()),
            incomplete -> none(incomplete.cast())
        );
      }
    };
  }

}
