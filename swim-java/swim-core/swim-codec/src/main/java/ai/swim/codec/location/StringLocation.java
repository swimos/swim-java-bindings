/*
 * Copyright 2015-2024 Swim Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.swim.codec.location;

import java.util.Objects;

public class StringLocation implements Location {

  private final int line;
  private final int column;
  private final int offset;

  public StringLocation(int line, int column, int offset) {
    this.line = line;
    this.column = column;
    this.offset = offset;
  }

  public static StringLocation of(int line, int column, int offset) {
    return new StringLocation(line, column, offset);
  }

  public int line() {
    return line;
  }

  public int column() {
    return column;
  }

  @Override
  public int offset() {
    return offset;
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
    return line == location.line && column == location.column && offset == location.offset;
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
        ", offset=" + offset +
        '}';
  }

}
