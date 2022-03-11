package ai.swim.codec.input;

import ai.swim.codec.Location;

public class InputDone implements Input {

  private final Input delegate;

  public InputDone(Input delegate) {
    this.delegate = delegate;
  }

  @Override
  public boolean complete() {
    return false;
  }

  @Override
  public boolean has(int n) {
    throw new IllegalStateException();
  }

  @Override
  public char head() {
    throw new IllegalStateException();
  }

  @Override
  public Input next() {
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
  public char[] collect() {
    return new char[]{};
  }

  @Override
  public int offset() {
    throw new IllegalStateException();
  }

  @Override
  public int len() {
    return this.delegate.len();
  }

  @Override
  public char[] take(int tagLength) {
    throw new IllegalStateException();
  }

  @Override
  public char[] borrow(int n) {
    throw new IllegalStateException();
  }

  @Override
  public boolean compare(char[] with) {
    return this.delegate.compare(with);
  }

  @Override
  public Input subInput(int start, int end) {
    return null;
  }

  @Override
  public Input advance(int m) {
    throw new IllegalStateException();
  }


}
