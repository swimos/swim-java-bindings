package ai.swim.codec.result;

import java.util.function.Function;
import ai.swim.codec.Cont;
import ai.swim.codec.input.Input;

public interface Result<O> {

  static <O> Result<O> ok(Input input, O output) {
    return new ParseOk<>(input, output);
  }

  static <O> Result<O> error(Input input, String cause) {
    return new ParseError<>(input, cause);
  }

  static <O> Result<O> incomplete(Input input, int needed) {
    return new ParseIncomplete<>(input, needed);
  }

  boolean isOk();

  boolean isError();

  boolean isIncomplete();

  Input getInput();

  default O getOutput() {
    throw new AssertionError();
  }

  <F> F match(Function<ParseOk<O>, F> ok, Function<ParseError<O>, F> error, Function<ParseIncomplete<O>, F> incomplete);

  default <O2> Result<O2> mapOk(Function<ParseOk<O>, Result<O2>> ok) {
    return this.cast();
  }

  @SuppressWarnings("unchecked")
  default <NO> Result<NO> cast() {
    return (Result<NO>) this;
  }

}

