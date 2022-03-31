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

import java.util.Optional;

public class OptParser<T> extends Parser<Optional<T>> {
  private Parser<T> inner;

  OptParser(Parser<T> inner) {
    this.inner = inner;
  }

  public static <T> Parser<Optional<T>> opt(Parser<T> parser) {
    return new OptParser<>(parser);
  }

  @Override
  public Parser<Optional<T>> feed(Input input) {
    if (input.isContinuation()) {
      Input innerInput = input.clone();
      Parser<T> result = this.inner.feed(innerInput);
      if (result.isError()) {
        // We failed to parse so there was no match. The parser could have failed because the token didn't match what
        // was available or because there was an input error. If it was the latter, as the input buffer isn't advanced
        // the error will be picked up again.
        return Parser.done(Optional.empty());
      } else if (result.isCont()) {
        // Insufficient input available.
        this.inner = result;
        return this;
      } else {
        // The parser succeeded. Advance the input buffer and return it's state.
        input.cloneFrom(innerInput);
        return Parser.done(Optional.ofNullable(result.bind()));
      }
    } else if (input.isError()) {
      return Parser.error(((InputError) input));
    } else if (input.isDone()) {
      return Parser.error(input, "Insufficient data");
    } else {
      return this;
    }
  }
}
