package ai.swim.recon.result;

public class ResultEnd<O> extends ParseResult<O> {
  @Override
  public boolean isDone() {
    return true;
  }

  @Override
  public <T> ParseResult<T> cast() {
    return new ResultEnd<>();
  }

  @Override
  public boolean equals(Object obj) {
    return super.equals(obj);
  }
}
