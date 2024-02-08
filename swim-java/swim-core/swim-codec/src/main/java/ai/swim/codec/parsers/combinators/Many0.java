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
import ai.swim.codec.input.Input;
import java.util.ArrayList;
import java.util.List;

public class Many0<T> extends Parser<List<T>> {
  private final Parser<T> delegate;
  private final List<T> output;
  private Parser<T> active;

  private Many0(Parser<T> delegate) {
    this.delegate = delegate;
    this.output = new ArrayList<>();
  }

  /**
   * Repeats the delegate parser zero or more times until it produces an error and returns the output as a list.
   *
   * @param delegate to apply.
   * @param <T>      the type the parser produces.
   * @return a list of the parser's output, an error or a continuation state.
   */
  public static <T> Parser<List<T>> many0(Parser<T> delegate) {
    return new Many0<>(delegate);
  }

  @Override
  public Parser<List<T>> feed(Input input) {
    while (true) {
      if (input.isContinuation()) {
        if (active == null) {
          active = delegate.feed(input);
        }
        if (active.isDone()) {
          output.add(active.bind());
          active = null;
        } else if (active.isError()) {
          return Parser.done(output);
        } else {
          return this;
        }
      } else if (input.isDone()) {
        return Parser.done(output);
      } else if (input.isEmpty()) {
        return this;
      } else {
        throw new AssertionError();
      }
    }
  }
}
