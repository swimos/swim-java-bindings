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

package ai.swim.codec.combinators;

import ai.swim.codec.Parser;
import ai.swim.codec.ParserError;
import ai.swim.codec.input.Input;

import java.util.function.Function;

public class AndThen<O, T> extends Parser<T> {
  private final Function<O, Parser<T>> then;
  private Parser<O> first;
  private Parser<T> second;

  AndThen(Parser<O> first, Function<O, Parser<T>> then) {
    this.first = first;
    this.then = then;
  }

  public static <O, T> Parser<T> andThen(Parser<O> first, Function<O, Parser<T>> then) {
    return new AndThen<>(first, then);
  }

  @Override
  public Parser<T> feed(Input input) {
    if (second == null) {
      Parser<O> parseResult = this.first.feed(input);
      if (parseResult.isDone()) {
        this.second = this.then.apply(parseResult.bind());
        return feed(input);
      } else if (parseResult.isCont()) {
        this.first = parseResult;
        return this;
      } else if (parseResult.isError()) {
        return Parser.error(input, ((ParserError<O>) parseResult).cause());
      } else {
        return this;
      }
    } else {
      if (second.isError()) {
        return Parser.error(input, ((ParserError<T>) second).cause());
      } else {
        Parser<T> result = this.second.feed(input);
        if (result.isDone()) {
          return Parser.done(result.bind());
        } else if (result.isError()) {
          return Parser.error(input, ((ParserError<T>) result).cause());
        } else {
          this.second = result;
          return this;
        }
      }
    }
  }
}
