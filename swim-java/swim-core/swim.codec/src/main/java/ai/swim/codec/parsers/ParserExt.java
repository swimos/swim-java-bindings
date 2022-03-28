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

  /***
   * Runs n parsers until one produces a value or every parser fails.
   **/
  @SafeVarargs
  public static <O> Parser<O> alt(Parser<O>... parsers) {
    return Parser.lambda(input -> {
      Parser<O> error = null;
      int errorCount = 0;

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
        } else if (parseResult.isDone()) {
          input.cloneFrom(source);
          return parseResult;
        }

        parsers[i] = parseResult;
      }

      if (errorCount == parsers.length) {
        return error;
      }

      return alt(parsers);
    });
  }

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

  private static class Many0State<O> {
    private final List<O> output;
    private Parser<O> parser;

    Many0State(List<O> output, Parser<O> parser) {
      this.output = output;
      this.parser = parser;
    }
  }

  public static <O> Parser<O> peek(Parser<O> parser) {
    return Parser.lambda(input -> parser.feed(input.clone()));
  }

}
