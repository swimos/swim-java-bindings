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

package ai.swim.codec.parsers.combinators;

import ai.swim.codec.Parser;
import ai.swim.codec.ParserError;
import ai.swim.codec.input.Input;

public class Chain<T> extends Parser<T> {
  private final Parser<?>[] parsers;
  private int idx;

  private Chain(Parser<?>[] parsers) {
    this.parsers = parsers;
    this.idx = 0;
  }

  /**
   * Runs n parsers and binds the final parser if it succeeds.
   */
  public static <T> Parser<T> chain(Parser<?>... parsers) {
    return new Chain<>(parsers);
  }

  @Override
  public Parser<T> feed(Input input) {
    while (true) {
      if (input.isContinuation()) {
        Parser<?> parser = parsers[idx];

        if (parser.isError()) {
          return Parser.error(input, ((ParserError<?>) parser).cause());
        }

        parser = parser.feed(input);

        if (parser.isError()) {
          return Parser.error(input, ((ParserError<?>) parser).cause());
        } else if (parser.isDone()) {
          if (idx == parsers.length - 1) {
            //noinspection unchecked
            return Parser.done((T) parser.bind());
          } else {
            parsers[idx] = parser;
            idx += 1;
          }
        } else if (parser.isCont()) {
          parsers[idx] = parser;
          return this;
        }
      } else if (input.isDone()) {
        Parser<?> parser = parsers[idx];
        if (parser.isCont()) {
          return Parser.error(input, "Expected more data");
        } else {
          throw new AssertionError("Unexpected state");
        }
      } else {
        return this;
      }
    }
  }
}
