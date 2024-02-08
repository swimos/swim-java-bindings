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

import ai.swim.codec.ParserError;
import ai.swim.codec.location.Location;

public abstract class ParseResult<O> {

  public static <O> ParseResult<O> ok(O item) {
    return new ResultOk<>(item);
  }

  public static <O> ParseResult<O> error(ParserError<O> parser) {
    return new ResultError<>(parser.cause(), parser.location());
  }

  public static <O> ParseResult<O> error(String cause, Location location) {
    return new ResultError<>(cause, location);
  }

  public static <O> ParseResult<O> continuation() {
    return new ResultContinuation<>();
  }

  public static <O> ParseResult<O> end() {
    return new ResultEnd<>();
  }

  public boolean isOk() {
    return false;
  }

  public boolean isError() {
    return false;
  }

  public boolean isCont() {
    return false;
  }

  public boolean isDone() {
    return false;
  }

  public abstract <T> ParseResult<T> cast();

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    return o != null && getClass() == o.getClass();
  }

  public O bind() {
    throw new IllegalStateException();
  }
}
