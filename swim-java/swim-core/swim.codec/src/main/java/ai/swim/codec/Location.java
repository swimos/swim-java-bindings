package ai.swim.codec;

import java.util.Objects;

public class Location {

  private final int line;
  private final int column;

  public Location(int line, int column) {
    this.line = line;
    this.column = column;
  }

  public static Location of(int line, int column) {
    return new Location(line, column);
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
    Location location = (Location) o;
    return line == location.line && column == location.column;
  }

  @Override
  public int hashCode() {
    return Objects.hash(line, column);
  }

  @Override
  public String toString() {
    return "Location{" +
        "line=" + line +
        ", column=" + column +
        '}';
  }

}
