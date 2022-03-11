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
      final Cont<? extends O> firstCont = first.apply(input);
      if (firstCont.isContinuation()) {
        return second.apply(input.next());
      } else {
        return none(Result.error(input, null));
      }
    };
  }

  public static <O, B> Parser<B> and(Parser<? extends O> first, Function<O, Parser<B>> second) {
    return input -> {
      final Cont<? extends O> fistCont = first.apply(input);
      if (fistCont.isContinuation()) {
        return continuation(() -> fistCont.getResult().match(
            ok -> second.apply(ok.getOutput()).apply(ok.getInput()).getResult(),
            Result::cast,
            Result::cast)
        );
      } else {
        return fistCont.getResult().match(
            ok -> {
              final Cont<B> secondCont = second.apply(ok.getOutput()).apply(ok.getInput());
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

  public static <O> Parser<Input> recognize(Parser<O> parser) {
    return input -> {
      int offset = input.offset();
      final Cont<O> fistCont = parser.apply(input);
      if (fistCont.isContinuation()) {
        return continuation(() -> fistCont.getResult().match(
            ok -> {
              int newOffset = ok.getInput().offset();
              return Result.ok(input.subInput(offset, newOffset), ok.getInput()).cast();
            },
            Result::cast,
            Result::cast)
        );
      } else {
        return fistCont.getResult().match(
            ok -> none(Result.ok(ok.getInput(), ok.getInput())),
            error -> none(error.cast()),
            incomplete -> none(incomplete.cast())
        );
      }
    };
  }

  public static class Indexed<O> {

    private final O output;
    private final int offset;

    public Indexed(O output, int offset) {
      this.output = output;
      this.offset = offset;
    }

    public O getOutput() {
      return output;
    }

    public int getOffset() {
      return offset;
    }

  }

}
