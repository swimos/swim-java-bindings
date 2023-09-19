package ai.swim.codec.input;

import ai.swim.codec.data.ReadBuffer;
import ai.swim.codec.location.Location;

public class ReadBufferInput extends Input {
  private static final int NO_LIMIT = -1;

  private ReadBuffer bytes;
  private boolean isPartial;
  private int limit;

  public ReadBufferInput(ReadBuffer bytes, boolean isPartial, int limit) {
    this.bytes = bytes;
    this.isPartial = isPartial;
    this.limit = limit;
  }

  public ReadBufferInput(ReadBuffer bytes) {
    this(bytes, false, NO_LIMIT);
  }

  public ReadBufferInput limit(int limit) {
    return new ReadBufferInput(bytes, isPartial, limit);
  }

  @Override
  public boolean has(int n) {
    if (n < 1) {
      throw new IllegalArgumentException("n < 1");
    }

    if (limit == NO_LIMIT) {
      return bytes.remaining() >= n;
    } else {
      return n <= limit - bytes.readPointer();
    }
  }

  @Override
  public int head() {
    return bytes.peekByte();
  }

  @Override
  public Input step() {
    if (isContinuation()) {
      bytes.advance(1);
      return this;
    } else {
      return Input.done(this);
    }
  }

  @Override
  public Location location() {
    return Location.of(0, 0, bytes.readPointer());
  }

  @Override
  public boolean isDone() {
    return !this.isPartial && !has(1);
  }

  @Override
  public boolean isContinuation() {
    return has(1);
  }

  @Override
  public boolean isEmpty() {
    return this.isPartial && this.bytes.remaining() != 0;
  }

  @Override
  public Input setPartial(boolean isPartial) {
    return new ReadBufferInput(bytes, isPartial, limit);
  }

  @Override
  public void bind(int[] into) {
    // todo: this method should be removed entirely
    throw new AssertionError();
  }

  @Override
  public int len() {
    if (limit == NO_LIMIT) {
      return bytes.remaining();
    } else {
      return bytes.remaining() - limit;
    }
  }

  @Override
  public void take(int[] into) {
    // todo: this method should be removed entirely
    throw new AssertionError();
  }

  @Override
  public Input clone() {
    return new ReadBufferInput(bytes.clone(), isPartial, limit);
  }

  @Override
  public Input extend(Input from) {
    if (from instanceof ReadBufferInput) {
      ReadBufferInput other = (ReadBufferInput) from;
      bytes.unsplit(other.bytes);
      return new ReadBufferInput(bytes, isPartial, limit);
    } else {
      throw new ClassCastException();
    }
  }

  @Override
  public void setFrom(Input from) {
    if (from instanceof ReadBufferInput) {
      ReadBufferInput other = (ReadBufferInput) from;
      this.bytes = other.bytes;
      this.limit = other.limit;
      this.isPartial = other.isPartial;
    } else {
      throw new ClassCastException();
    }
  }
}
