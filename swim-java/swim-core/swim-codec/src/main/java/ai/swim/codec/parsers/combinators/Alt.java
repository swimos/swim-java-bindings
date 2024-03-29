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

public class Alt<T> extends Parser<T> {
  private final Parser<T>[] parsers;
  private Integer last;

  @SafeVarargs
  private Alt(Parser<T>... parsers) {
    this.parsers = parsers;
  }

  /**
   * Tests n parsers with a clone of the input until one makes progress.
   * <p>
   * An alternating parser has one of four states:
   * - A parser produces a value; it is in the done state. If this condition is met then the parser itself is returned
   * and the input is advanced.
   * - More than one parser is in a continuation state. If this condition is met then the alt parser itself is returned
   * with the set of parsers provided. In this state, the input will not be advanced.
   * - A single parser is in the continuation state and every other parser produced an error. If this condition is met
   * then the parser itself is returned. In this state, the input will be advanced before the parser is returned.
   * - Every parser produced an error. The last error will be returned and the input will not be advanced.
   * <p>
   * The parsers provided must be able to determine if they are able to make progress as early as possible to determine
   * which parser to return out of the set. If this condition is not met then the input will not be advanced
   * accordingly.
   *
   * @param parsers to alternative between.
   * @param <T>     the type that the parsers produce.
   * @return see method documentation.
   */
  @SafeVarargs
  public static <T> Parser<T> alt(Parser<T>... parsers) {
    return new Alt<>(parsers);
  }

  @Override
  public Parser<T> feed(Input input) {
    Parser<T> error = null;
    Parser<T> cont = null;
    Input advanced = null;
    int errorCount = 0;
    int contCount = 0;

    // try running the last successful parser first as a fast path
    if (last != null) {
      Parser<T> p = parsers[last];
      if (!p.isError()) {
        Input source = input.clone();
        Parser<T> result = p.feed(source);

        if (result.isError()) {
          errorCount += 1;
          error = result;
        } else if (result.isCont()) {
          contCount += 1;
          cont = result;
          advanced = source;
        } else {
          input.setFrom(source);
          return result;
        }

        parsers[last] = result;
      }
    }

    for (int i = 0; i < parsers.length; i++) {
      if (last != null && last == i) {
        // In order to reach this branch, the 'last' parser would have failed and so it can be skipped.
        continue;
      }

      Parser<T> p = parsers[i];

      if (p.isError()) {
        continue;
      }

      Input source = input.clone();
      Parser<T> parseResult = p.feed(source);

      if (parseResult.isError()) {
        errorCount += 1;
        error = parseResult;
      } else if (parseResult.isCont()) {
        // It's possible that a branch cannot make progress due to insufficient input and will return a continuation.
        // This is problematic as that branch could be returned and effectively starve the other branch in the
        // combinator. We want every branch to try and make some progress before deciding which branch to return.
        contCount += 1;
        cont = parseResult;
        advanced = source;
      } else if (parseResult.isDone()) {
        // Set the last index so in the next invocation we can run it as a fast path.
        last = i;

        input.setFrom(source);
        return parseResult;
      }

      parsers[i] = parseResult;
    }

    if (errorCount == parsers.length) {
      return error;
    }

    if (contCount == parsers.length || cont == null) {
      /// There was insufficient data available for any branches to make progress.
      return this;
    }

    input.setFrom(advanced);
    return cont;
  }
}
