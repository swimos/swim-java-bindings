package ai.swim.codec.input;

import ai.swim.codec.data.ByteReader;
import ai.swim.codec.location.Location;

public class ByteReaderInput extends Input {
  private static final int NO_LIMIT = -1;

  private ByteReader bytes;
  private boolean isPartial;
  private int limit;

  public ByteReaderInput(ByteReader bytes, boolean isPartial, int limit) {
    this.bytes = bytes;
    this.isPartial = isPartial;
    this.limit = limit;
  }

  public ByteReaderInput(ByteReader bytes) {
    this(bytes, false, NO_LIMIT);
  }

  public ByteReaderInput limit(int limit) {
    return new ByteReaderInput(bytes, isPartial, limit);
  }

  @Override
  public boolean has(int n) {
    if (limit == NO_LIMIT) {
      int remaining = bytes.remaining();
      if (remaining == 1) {
        remaining = 0;
      }

      // This class lags behind by 1 as the `head()` peeks the next byte. If there is only one byte remaining, then
      // we're done.

      return remaining >= n;
    } else {
      return n <= limit - bytes.getReadPointer();
    }
  }

  @Override
  public int head() {
    return bytes.peekByte();
  }

  @Override
  public Input step() {
    if (has(1)) {
      bytes.advance(1);
      return this;
    } else {
      return Input.done(this);
    }
  }

  @Override
  public Location location() {
    return Location.of(0, 0, bytes.getReadPointer());
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
    return new ByteReaderInput(bytes, isPartial, limit);
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
    return new ByteReaderInput(bytes.clone(), isPartial, limit);
  }

  @Override
  public Input extend(Input from) {
    if (from instanceof ByteReaderInput) {
      ByteReaderInput other = (ByteReaderInput) from;
      bytes.unsplit(other.bytes);
      return new ByteReaderInput(bytes, isPartial, limit);
    } else {
      throw new ClassCastException();
    }
  }

  @Override
  public void setFrom(Input from) {
    if (from instanceof ByteReaderInput) {
      ByteReaderInput other = (ByteReaderInput) from;
      this.bytes = other.bytes;
      this.limit = other.limit;
      this.isPartial = other.isPartial;
    } else {
      throw new ClassCastException();
    }
  }
}
