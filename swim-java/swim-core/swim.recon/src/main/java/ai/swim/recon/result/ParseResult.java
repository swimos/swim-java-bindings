package ai.swim.recon.result;

import ai.swim.codec.ParserError;
import ai.swim.codec.input.InputError;

public abstract class ParseResult<O> {

  public static <O> ParseResult<O> ok(O item) {
    return new ResultOk<>(item);
  }

  public static <O> ParseResult<O> error(String cause) {
    return new ResultError<>(cause);
  }

  public static <O> ParseResult<O> error(InputError error) {
    return new ResultError<>(error.getCause());
  }

  public static <O> ParseResult<O> error(ParserError<O> parser) {
    return new ResultError<>(parser.getCause());
  }

  public static <O> ParseResult<O> continuation() {
    return new ResultContinuation<>();
  }

}
