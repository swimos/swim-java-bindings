package ai.swim.codec.input;

import ai.swim.codec.location.Location;
import ai.swim.codec.location.StringLocation;
import java.util.Objects;

public class StringInput extends Input {

  private String data;
  private int line;
  private int column;
  private int index;
  private int offset;
  private boolean isPartial;

  public StringInput(String data, int line, int column, int index, int offset) {
    this.data = data;
    this.line = line;
    this.column = column;
    this.index = index;
    this.offset = offset;
    this.isPartial = false;
  }

  StringInput(String data) {
    this(data, 1, 1, 0, 0);
  }

  public static String codePointsToString(int[] codePoints) {
    StringBuilder sb = new StringBuilder(codePoints.length);
    for (int c : codePoints) {
      sb.appendCodePoint(c);
    }

    return sb.toString();
  }

  @Override
  public boolean has(int n) {
    return (offset + n) <= data.length();
  }

  @Override
  public int head() {
    if (this.index < this.data.length()) {
      return this.data.codePointAt(this.index);
    } else {
      throw new IllegalStateException();
    }
  }

  @Override
  public Input step() {
    final int index = this.index;
    if (index < this.data.length()) {
      advance();
      return this;
    } else {
      return Input.done(this);
    }
  }

  @Override
  public Location location() {
    return new StringLocation(this.line, this.column, offset);
  }

  @Override
  public boolean isDone() {
    return !this.isPartial && this.index >= this.data.length();
  }

  @Override
  public boolean isContinuation() {
    return this.index < this.data.length();
  }

  @Override
  public boolean isEmpty() {
    return this.isPartial && this.index >= this.data.length();
  }

  @Override
  public Input setPartial(boolean isPartial) {
    this.isPartial = isPartial;
    return this;
  }

  @Override
  public void bind(int[] into) {
    if (this.isDone()) {
      return;
    }

    int len = Math.min(this.len(), into.length);

    for (int i = 0; i < len; i++) {
      int cursor = this.index + i;
      into[i] = this.data.codePointAt(cursor);
    }
  }

  @Override
  public String toString() {
    return "StringSource{" +
        "data='" + data + '\'' +
        ", line=" + line +
        ", column=" + column +
        ", index=" + index +
        ", offset=" + offset +
        '}';
  }

  @Override
  public int len() {
    return this.data.length() - this.offset;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StringInput that = (StringInput) o;
    return line == that.line && column == that.column && index == that.index && offset == that.offset && Objects.equals(
        data,
        that.data);
  }

  @Override
  public int hashCode() {
    return Objects.hash(data, line, column, index, offset);
  }

  @Override
  public void take(int[] into) {
    int n = into.length;

    if (!this.has(n)) {
      throw new IllegalStateException();
    }

    for (int i = 0; i < n; i++) {
      into[i] = this.advance();
    }
  }

  @Override
  public Input clone() {
    return new StringInput(this.data, this.line, this.column, this.index, this.offset).setPartial(this.isPartial);
  }

  @Override
  public void setFrom(Input input) {
    if (input instanceof StringInput) {
      // Strip off any already processed tokens and reset the markers back to zero.

      StringInput stringInput = (StringInput) input;
      this.isPartial = stringInput.isPartial;
      this.data = stringInput.data.substring(stringInput.index);
      this.column = stringInput.column;
      this.index = 0;
      this.line = stringInput.line;
      this.offset = 0;
    } else {
      throw new UnsupportedOperationException("Cannot extend a StringInput from a: " + input
          .getClass()
          .getCanonicalName());
    }
  }

  @Override
  public Input extend(Input from) {
    Objects.requireNonNull(from);
    if (from instanceof StringInput) {
      StringInput stringInput = (StringInput) from;
      return new StringInput(this.data + stringInput.data, this.line, this.column, this.index, this.offset).setPartial(
          stringInput.isPartial);
    } else {
      throw new IllegalArgumentException("Cannot extend a StringInput from a: " + from.getClass().getCanonicalName());
    }
  }

  private int advance() {
    final int idx = this.index;
    final int c = this.data.codePointAt(idx);

    this.index = this.data.offsetByCodePoints(idx, 1);
    this.offset += (long) (this.index - idx);

    if (c == '\n') {
      this.line += 1;
      this.column = 1;
    } else {
      this.column += 1;
    }

    return c;
  }

}
