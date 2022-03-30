package ai.swim.codec.input;

import ai.swim.codec.location.Location;

public class InputDone extends Input {

  private final Input delegate;

  public InputDone(Input delegate) {
    this.delegate = delegate;
  }

  @Override
  public boolean has(int n) {
    throw new IllegalStateException();
  }

  @Override
  public int head() {
    throw new IllegalStateException();
  }

  @Override
  public Input step() {
    throw new IllegalStateException();
  }

  @Override
  public Location location() {
    return this.delegate.location();
  }

  @Override
  public boolean isDone() {
    return true;
  }

  @Override
  public boolean isContinuation() {
    return false;
  }

  @Override
  public boolean isEmpty() {
    return false;
  }

  @Override
  public Input isPartial(boolean isPartial) {
    return this;
  }

  @Override
  public boolean isError() {
    return false;
  }

  @Override
  public int[] bind() {
    return new int[]{};
  }

  @Override
  public int len() {
    return this.delegate.len();
  }

  @Override
  public int[] take(int tagLength) {
    throw new IllegalStateException();
  }

  @Override
  public boolean compare(int[] with) {
    return this.delegate.compare(with);
  }

  @Override
  public Input clone() {
    return new InputDone(this.delegate.clone());
  }

  @Override
  public Input extend(Input from) {
    return this.delegate.extend(from);
  }

}
