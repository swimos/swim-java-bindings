package ai.swim.codec.input;

import ai.swim.codec.Location;

public class StringInput implements Input {

  private final String data;
  private int line;
  private int column;
  private int index;
  private int offset;

  StringInput(String data, int line, int column, int index, int offset) {
    this.data = data;
    this.line = line;
    this.column = column;
    this.index = index;
    this.offset = offset;
  }

  StringInput(String data) {
    this(data, 1, 1, 0, 0);
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
  public char head() {
    if (this.index < this.data.length()) {
      return this.data.charAt(this.index);
    } else {
      throw new IllegalStateException();
    }
  }

  @Override
  public Input next() {
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
    return new Location(this.line, this.column);
  }

  @Override
  public boolean isDone() {
    return index >= this.data.length();
  }

  @Override
  public char[] collect() {
    if (this.isDone()) {
      return new char[] {};
    }

    return this.data.substring(this.offset).toCharArray();
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
  public char[] take(int n) {
    if (!this.has(n)) {
      throw new IllegalStateException();
    }

    char[] output = new char[n];

    for (int i = 0; i < n; i++) {
      output[i] = this.advance();
    }

    return output;
  }

  @Override
  public char[] borrow(int n) {
    if (!this.has(n)) {
      throw new IllegalStateException();
    }

    return this.data.substring(this.offset, this.offset + n).toCharArray();
  }

  @Override
  public boolean compare(char[] with) {
    throw new IllegalStateException();
  }

  @Override
  public Input subInput(int start, int end) {
    return new StringInput(
        this.data.substring(start, end),
        1, 1, 0, 0
    );
  }

  @Override
  public Input advance(int n) {
    if (!this.has(n)) {
      throw new IllegalStateException();
    }

    for (int i = 0; i < n; i++) {
      this.advance();
    }

    return this;
  }


  private char advance() {
    final int idx = this.index;
    final char c = this.data.charAt(idx);

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
