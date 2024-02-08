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

public class Peek<T> extends Parser<T> {
  private Parser<T> parser;

  private Peek(Parser<T> parser) {
    this.parser = parser;
  }

  /**
   * Applies a parser without advancing its input.
   *
   * @param delegate to apply
   * @param <T>      the type the parser produces.
   * @return the parser's output, an error or a continuation.
   */
  public static <T> Parser<T> peek(Parser<T> delegate) {
    return new Peek<>(delegate);
  }

  @Override
  public Parser<T> feed(Input input) {
    parser = parser.feed(input.clone());
    if (parser.isCont()) {
      return this;
    } else {
      return parser;
    }
  }
}
