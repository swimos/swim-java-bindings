package ai.swim.codec.result;

import java.util.function.Function;
import ai.swim.codec.source.Source;

public interface Result<O> {

  static <O> Result<O> ok(Source source, O output) {
    return new ParseOk<>(source, output);
  }

  static <O> Result<O> error(Source source, String cause) {
    return new ParseError<>(source, cause);
  }

  static <O> Result<O> incomplete(Source source, int needed) {
    return new ParseIncomplete<>(source, needed);
  }

  boolean isOk();

  boolean isError();

  boolean isIncomplete();

  Source getInput();

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

