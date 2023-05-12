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

package ai.swim.codec;

import ai.swim.codec.input.Input;
import ai.swim.codec.location.Location;

public class ParserError<O> extends Parser<O> {
  private final String cause;
  private final Location location;

  public ParserError(Location location, String cause) {
    this.location = location;
    this.cause = cause;
  }

  @Override
  public Parser<O> feed(Input input) {
    throw bail();
  }

  /**
   * Returns the location that this error was produced.
   */
  public Location location() {
    return location;
  }

  /**
   * Returns the cause of this error.
   */
  public String cause() {
    return cause;
  }

  @Override
  public boolean isError() {
    return true;
  }

  @Override
  public boolean isCont() {
    return false;
  }

  @Override
  public O bind() {
    throw bail();
  }

  private IllegalStateException bail() {
    return new IllegalStateException(
      String.format("Parser error caused by %s at: %s", cause, location)
    );
  }
}
