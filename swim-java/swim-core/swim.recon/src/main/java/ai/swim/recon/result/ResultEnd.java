package ai.swim.recon.result;

import ai.swim.recon.event.ReadEvent;

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
