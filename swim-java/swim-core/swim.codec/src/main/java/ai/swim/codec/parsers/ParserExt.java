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
import ai.swim.codec.input.Input;
import ai.swim.codec.input.InputError;
import ai.swim.codec.parsers.stateful.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class ParserExt {

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
   * @param <O>     the type that the parsers produce.
   * @return see method documentation.
   */
  @SafeVarargs
  public static <O> Parser<O> alt(Parser<O>... parsers) {
    return Parser.lambda(input -> {
      Parser<O> error = null;
      Parser<O> cont = null;
      Input advanced = null;
      int errorCount = 0;
      int contCount = 0;

      for (int i = 0; i < parsers.length; i++) {
        Parser<O> p = parsers[i];

        if (p.isError()) {
          continue;
        }

        Input source = input.clone();
        Parser<O> parseResult = p.feed(source);

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
          input.cloneFrom(source);
          return parseResult;
        }

        parsers[i] = parseResult;
      }

      if (errorCount == parsers.length) {
        return error;
      }

      if (contCount == parsers.length || cont == null) {
        /// There was insufficient data available for any branches to make progress.
        return alt(parsers);
      }

      input.cloneFrom(advanced);
      return cont;
    });
  }

  /**
   * Applies the predicate 0 or more times to the input and produces a string representation of its output.
   *
   * @param predicate to apply against the input's head.
   * @return a string representation of the input up until the parser failed.
   */
  public static Parser<String> takeWhile0(Predicate<Character> predicate) {
    return Parser.stateful(new StringBuilder(), (state, input) -> {
      while (true) {
        if (input.isDone()) {
          return Result.ok(state.toString());
        } else if (input.isError()) {
          return Result.err(((InputError) input).getCause());
        } else if (input.isContinuation()) {
          int c = input.head();
          if (predicate.test((char) c)) {
            state.appendCodePoint(c);
            input = input.step();
          } else {
            return Result.ok(state.toString());
          }
        } else {
          return Result.cont(state);
        }
      }
    });
  }

  /**
   * Repeats the delegate parser zero or more times until it produces an error and returns the output as a list.
   *
   * @param delegate to apply.
   * @param <O>      the type the parser produces.
   * @return a list of the parser's output, an error or a continuation state.
   */
  public static <O> Parser<List<O>> many0(Parser<O> delegate) {
    return Parser.stateful(new Many0State<>(new ArrayList<>(), delegate), (state, input) -> {
      while (true) {
        if (input.isDone()) {
          return Result.ok(state.output);
        } else if (input.isError()) {
          return Result.err(((InputError) input).getCause());
        } else if (input.isContinuation()) {
          Parser<O> result = state.parser.feed(input);
          if (result.isError()) {
            return Result.ok(state.output);
          } else if (result.isCont()) {
            state.parser = result;
            return Result.cont(state);
          } else if (result.isDone()) {
            state.output.add(result.bind());
          } else {
            throw new AssertionError();
          }
        } else {
          return Result.cont(state);
        }
      }
    });
  }

  /**
   * Applies a parser without advancing its input.
   *
   * @param parser to apply
   * @param <O>    the type the parser produces.
   * @return the parser's output, an error or a continuation.
   */
  public static <O> Parser<O> peek(Parser<O> parser) {
    return Parser.lambda(input -> parser.feed(input.clone()));
  }

  private static class Many0State<O> {
    private final List<O> output;
    private Parser<O> parser;

    Many0State(List<O> output, Parser<O> parser) {
      this.output = output;
      this.parser = parser;
    }
  }

}
