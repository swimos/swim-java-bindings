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
  public int head() {
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
  public boolean isError() {
    return false;
  }

  @Override
  public int[] collect() {
    return new int[] {};
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
  public int[] take(int tagLength) {
    throw new IllegalStateException();
  }

  @Override
  public int[] borrow(int n) {
    throw new IllegalStateException();
  }

  @Override
  public boolean compare(int[] with) {
    return this.delegate.compare(with);
  }

  @Override
  public Source slice(int start, int end) {
    return null;
  }

  @Override
  public Source advance(int m) {
    throw new IllegalStateException();
  }


}
