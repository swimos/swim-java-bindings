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
import ai.swim.codec.input.InputError;

public class Preceded<B,T> extends Parser<T> {

  private Parser<B> by;
  private final Parser<T> then;

  public Preceded(Parser<B> by, Parser<T> then) {
    this.by = by;
    this.then = then;
  }

  @Override
  public Parser<T> feed(Input input) {
    if (input.isDone()) {
      return Parser.error("Not enough input");
    } else if (input.isError()) {
      return Parser.error(((InputError) input).getCause());
    } else if (input.isContinuation()) {
      Parser<B> result = this.by.feed(input);
      if (result.isError()) {
        return Parser.error(((ParserError<B>)result).getCause());
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

  public static <B,T> Parser<T> preceded(Parser<B> by, Parser<T> then) {
    return new Preceded<>(by, then);
  }

}
