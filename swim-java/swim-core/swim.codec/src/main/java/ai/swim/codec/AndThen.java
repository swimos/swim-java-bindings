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
import java.util.function.Function;

public class AndThen<O, T> extends Parser<T> {
  private Parser<O> first;
  private final Function<O, Parser<T>> then;

  AndThen(Parser<O> first, Function<O, Parser<T>> then) {
    this.first = first;
    this.then = then;
  }

  public static <O, T> Parser<T> andThen(Parser<O> first, Function<O, Parser<T>> then) {
    return new AndThen<>(first, then);
  }

  @Override
  public Parser<T> feed(Input input) {
    Parser<O> parseResult = this.first.feed(input);
    if (parseResult.isDone()) {
      return this.then.apply(parseResult.bind());
    } else if (parseResult.isCont()) {
      this.first = parseResult;
      return this;
    } else {
      return Parser.error(((ParserError<O>) parseResult).getCause());
    }
  }
}
