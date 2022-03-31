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

package ai.swim.codec.parsers;

import ai.swim.codec.Parser;
import ai.swim.codec.ParserError;
import ai.swim.codec.input.Input;
import ai.swim.codec.input.InputError;

public class Preceded<B, T> extends Parser<T> {

  private final Parser<T> then;
  private Parser<B> by;

  public Preceded(Parser<B> by, Parser<T> then) {
    this.by = by;
    this.then = then;
  }

  public static <B, T> Parser<T> preceded(Parser<B> by, Parser<T> then) {
    return new Preceded<>(by, then);
  }

  @Override
  public Parser<T> feed(Input input) {
    if (input.isDone()) {
      return Parser.error(input, "Not enough input");
    } else if (input.isError()) {
      return Parser.error(((InputError) input));
    } else if (input.isContinuation()) {
      Parser<B> result = this.by.feed(input);
      if (result.isError()) {
        return Parser.error(input, ((ParserError<B>) result).cause());
      } else if (result.isDone()) {
        return this.then.feed(input);
      } else {
        this.by = result;
        return this;
      }
    } else {
      return this;
    }
  }

}
