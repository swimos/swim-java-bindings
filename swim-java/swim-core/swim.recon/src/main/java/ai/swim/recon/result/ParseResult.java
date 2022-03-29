package ai.swim.recon.result;

import ai.swim.codec.ParserError;
import ai.swim.codec.input.InputError;
import ai.swim.recon.event.ReadEvent;

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

  public static <O> ParseResult<O> end() {
    return new ResultEnd<>();
  }

  public boolean isOk() {
    return false;
  }

  public boolean isError() {
    return false;
  }

  public boolean isCont() {
    return false;
  }

  public boolean isDone(){return false;}

  public abstract <T> ParseResult<T> cast();

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    return o != null && getClass() == o.getClass();
  }

  public O bind() {
    throw new IllegalStateException();
  }
}
