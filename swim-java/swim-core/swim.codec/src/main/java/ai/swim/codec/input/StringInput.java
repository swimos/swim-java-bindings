package ai.swim.codec.input;

import ai.swim.codec.Location;

public class StringInput implements Input<Character> {

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
    this(data, 0, 0, 0, 0);
  }

  @Override
  public boolean complete() {
    return index >= data.length();
  }

  @Override
  public Character head() {
    if (this.index < this.data.length()) {
      return this.data.charAt(this.index);
    } else {
      throw new IllegalStateException();
    }
  }

  @Override
  public Input<Character> next() {
    final int index = this.index;
    if (index < this.data.length()) {
      final int c = this.data.charAt(index);
      this.index = this.data.offsetByCodePoints(index, 1);
      this.offset += (long) (this.index - index);
      if (c == '\n') {
        this.line += 1;
        this.column = 1;
      } else {
        this.column += 1;
      }
      return this;
    } else {
      return Input.done(new Location(this.line, this.column));
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

}
