package ai.swim.recon.result;

public class ResultContinuation<O> extends ParseResult<O> {

  @Override
  public boolean isCont() {
    return true;
  }

  @Override
  public <T> ParseResult<T> cast() {
    return new ResultContinuation<>();
  }
}
