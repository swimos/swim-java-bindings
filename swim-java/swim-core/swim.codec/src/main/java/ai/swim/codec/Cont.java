package ai.swim.codec;

import java.util.function.Supplier;


public interface Cont<I, O> {

  static <I, O> Cont<I, O> continuation(Supplier<Result<I, O>> supplier) {
    return new Continuation<>(supplier);
  }

  static <I, O> Cont<I, O> none(Result<I, O> result) {
    return new None<>(result);
  }

  boolean isContinuation();

  Result<I, O> getResult();

}

final class Continuation<I, O> implements Cont<I, O> {

  private Supplier<Result<I, O>> supplier;
  private Result<I, O> result;

  Continuation(Supplier<Result<I, O>> supplier) {
    this.supplier = supplier;
  }

  @Override
  public boolean isContinuation() {
    return true;
  }

  @Override
  public Result<I, O> getResult() {
    if (supplier != null) {
      result = supplier.get();
      supplier = null;
    }

    return result;
  }

}

final class None<I, O> implements Cont<I, O> {

  private final Result<I, O> result;

  None(Result<I, O> result) {
    this.result = result;
  }

  @Override
  public boolean isContinuation() {
    return false;
  }

  @Override
  public Result<I, O> getResult() {
    return result;
  }

}
