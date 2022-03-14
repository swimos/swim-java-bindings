package ai.swim.codec.source;

import ai.swim.codec.Location;

public class SourceDone implements Source {

  private final Source delegate;

  public SourceDone(Source delegate) {
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
  public Source next() {
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
    return new char[] {};
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
  public Source subInput(int start, int end) {
    return null;
  }

  @Override
  public Source advance(int m) {
    throw new IllegalStateException();
  }


}
