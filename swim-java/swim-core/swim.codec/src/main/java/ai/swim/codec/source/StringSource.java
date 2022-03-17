package ai.swim.codec.source;

import java.util.Objects;
import ai.swim.codec.Location;
import ai.swim.codec.StringLocation;

public class StringSource implements Source {

  private final String data;
  private int line;
  private int column;
  private int index;
  private int offset;

  public StringSource(String data, int line, int column, int index, int offset) {
    this.data = data;
    this.line = line;
    this.column = column;
    this.index = index;
    this.offset = offset;
  }

  StringSource(String data) {
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
  public boolean complete() {
    return index >= data.length();
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
  public Source next() {
    final int index = this.index;
    if (index < this.data.length()) {
      advance();
      return this;
    } else {
      return Source.done(this);
    }
  }

  @Override
  public Location location() {
    return new StringLocation(this.line, this.column);
  }

  @Override
  public boolean isDone() {
    return index >= this.data.length();
  }

  @Override
  public boolean isError() {
    return false;
  }

  @Override
  public int[] collect() {
    if (this.isDone()) {
      return new int[] {};
    }

    return this.data.substring(this.offset).chars().toArray();
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
  public int offset() {
    return this.offset;
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
    StringSource that = (StringSource) o;
    return line == that.line && column == that.column && index == that.index && offset == that.offset && Objects.equals(data, that.data);
  }

  @Override
  public int hashCode() {
    return Objects.hash(data, line, column, index, offset);
  }

  @Override
  public int[] take(int n) {
    if (!this.has(n)) {
      throw new IllegalStateException();
    }

    int[] output = new int[n];

    for (int i = 0; i < n; i++) {
      output[i] = this.advance();
    }

    return output;
  }

  @Override
  public int[] borrow(int n) {
    if (!this.has(n)) {
      throw new IllegalStateException();
    }

    return this.data.substring(this.offset, this.offset + n).chars().toArray();
  }

  @Override
  public boolean compare(int[] with) {
    throw new IllegalStateException();
  }

  @Override
  public Source slice(int start, int end) {
    return new StringSource(
        this.data.substring(start, end),
        1, 1, 0, 0
    );
  }

  @Override
  public Source advance(int n) {
    if (!this.has(n)) {
      throw new IllegalStateException();
    }

    for (int i = 0; i < n; i++) {
      this.advance();
    }

    return this;
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
