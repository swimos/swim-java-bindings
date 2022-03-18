// Copyright 2015-2021 Swim Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package ai.swim.codec.location;

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
