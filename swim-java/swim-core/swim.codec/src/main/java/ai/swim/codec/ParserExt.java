package ai.swim.codec;

import java.util.function.Function;
import java.util.function.Predicate;
import ai.swim.codec.input.Input;
import ai.swim.codec.result.Result;
import static ai.swim.codec.Cont.continuation;
import static ai.swim.codec.Cont.none;

public class ParserExt {

  public static <O> Parser<O> then(O x) {
    return input -> none(Result.ok(input, x));
  }

  public static <O> Parser<O> eq(String value) {
    return pred(value::equals);
  }

  public static <O> Parser<O> pred(Predicate<String> predicate) {
    return input -> {
      if (input.complete()) {
        return none(Result.incomplete(input, 1));
      } else {
        final char head = input.head();
        if (predicate.test(String.valueOf(head))) {
          final Input newInput = input.next();
          return continuation(() -> Result.ok(newInput, head).cast());
        } else {
          return none(Result.error(input, null));
        }
      }
    };
  }

  public static <O> Parser<O> preceded(Parser<O> first, Parser<O> second) {
    return input -> {
      final Cont<O> cont = first.apply(input);
      if (cont.isContinuation()) {
        return second.apply(input.next());
      } else {
        return cont;
      }
    };
  }

  public static <O, B> Parser<B> and(Parser<O> first, Function<O, Parser<B>> second) {
    return input -> {
      final Cont<O> firstCont = first.apply(input);
      if (firstCont.isContinuation()) {
        return continuation(() -> firstCont.getResult().match(
            ok -> second.apply(ok.getOutput()).apply(ok.getInput()).getResult(),
            Result::cast,
            Result::cast)
        );
      } else {
        Result<B> result = firstCont.getResult().mapOk(ok -> second.apply(ok.getOutput()).apply(ok.getInput()).getResult());
        if (result.isOk()) {
          return continuation(() -> result);
        } else {
          return none(result);
        }
      }
    };
  }

  public static <O> Parser<Input> recognize(Parser<O> parser) {
    return input -> {
      int offset = input.offset();
      final Cont<O> cont = parser.apply(input);
      if (cont.isContinuation()) {
        return continuation(() -> cont.getResult().match(
            ok -> {
              int newOffset = ok.getInput().offset();
              return Result.ok(input.subInput(offset, newOffset), ok.getInput()).cast();
            },
            Result::cast,
            Result::cast)
        );
      } else {
        return cont.getResult().match(
            ok -> none(Result.ok(ok.getInput(), ok.getInput())),
            error -> none(error.cast()),
            incomplete -> none(incomplete.cast())
        );
      }
    };
  }

  public static <O> Parser<O> alt(Parser<O>... parsers) {
    if (parsers.length == 0) {
      throw new IllegalStateException();
    }

    return input -> {
      if (input.complete()) {
        return none(Result.incomplete(input, 1));
      } else {
        Result<O> err=null;
        for (Parser<O> parser : parsers) {
          Result<O> result = parser.parse(input);

          if (result.isOk()) {
            return continuation(() -> result);
          } else {
            err = result;
          }
        }

        return none(err);
      }
    };
  }

}
