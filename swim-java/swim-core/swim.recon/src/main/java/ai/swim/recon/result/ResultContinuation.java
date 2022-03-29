package ai.swim.recon.result;

import ai.swim.recon.event.ReadEvent;

public class ResultContinuation<O> extends ParseResult<O> {

  @Override
  public boolean isCont() {
    return true;
  }

  @Override
  public <T>ParseResult<T> cast() {
    return new ResultContinuation<>();
  }
}
