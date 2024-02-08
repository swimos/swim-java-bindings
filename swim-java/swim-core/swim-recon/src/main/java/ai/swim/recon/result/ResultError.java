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

package ai.swim.recon.result;

import ai.swim.codec.location.Location;

public class ResultError<O> extends ParseResult<O> {
  private final String cause;
  private final Location location;

  public ResultError(String cause, Location location) {
    this.cause = cause;
    this.location = location;
  }

  public String getCause() {
    return cause;
  }

  public Location getLocation() {
    return location;
  }

  @Override
  public boolean isError() {
    return true;
  }

  @Override
  public <T> ParseResult<T> cast() {
    return new ResultError<>(cause, location);
  }

  @Override
  public String toString() {
    return "ResultError{" +
        "cause='" + cause + '\'' +
        ", location=" + location +
        '}';
  }
}
