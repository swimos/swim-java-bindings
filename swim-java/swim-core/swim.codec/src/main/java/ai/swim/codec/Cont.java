package ai.swim.codec;

import java.util.function.Supplier;
import ai.swim.codec.result.Result;


public interface Cont<O> {

  static <O> Cont<O> continuation(Supplier<Result<O>> supplier) {
    return new Continuation<>(supplier);
  }

  static <O> Cont<O> none(Result<O> result) {
    return new None<>(result);
  }

  boolean isContinuation();

  Result<O> getResult();

}

final class Continuation<O> implements Cont<O> {

  private Supplier<Result<O>> supplier;
  private Result<O> result;

  Continuation(Supplier<Result<O>> supplier) {
    this.supplier = supplier;
  }

  @Override
  public boolean isContinuation() {
    return true;
  }

  @Override
  public Result<O> getResult() {
    if (supplier != null) {
      result = supplier.get();
      supplier = null;
    }

    return result;
  }

}

final class None<O> implements Cont<O> {

  private final Result<O> result;

  None(Result<O> result) {
    this.result = result;
  }

  @Override
  public boolean isContinuation() {
    return false;
  }

  @Override
  public Result<O> getResult() {
    return result;
  }

}
