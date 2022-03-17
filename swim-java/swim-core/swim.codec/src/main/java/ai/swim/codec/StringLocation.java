package ai.swim.codec;

import java.util.Objects;

public class StringLocation implements Location {

  private final int line;
  private final int column;

  public StringLocation(int line, int column) {
    this.line = line;
    this.column = column;
  }

  public static StringLocation of(int line, int column) {
    return new StringLocation(line, column);
  }

  public int getLine() {
    return line;
  }

  public int getColumn() {
    return column;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StringLocation location = (StringLocation) o;
    return line == location.line && column == location.column;
  }

  @Override
  public int hashCode() {
    return Objects.hash(line, column);
  }

  @Override
  public String toString() {
    return "StringLocation{" +
        "line=" + line +
        ", column=" + column +
        '}';
  }

}
