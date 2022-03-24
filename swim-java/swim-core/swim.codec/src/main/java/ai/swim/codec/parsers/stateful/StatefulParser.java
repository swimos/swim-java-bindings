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

package ai.swim.codec.parsers.stateful;

import ai.swim.codec.Parser;
import ai.swim.codec.input.Input;
import java.util.function.BiFunction;

public class StatefulParser<S, T> extends Parser<T> {
  private final BiFunction<S, Input, Result<S, T>> parser;
  private S state;

  public StatefulParser(S state, BiFunction<S, Input, Result<S, T>> parser) {
    this.state = state;
    this.parser = parser;
  }

  @Override
  public Parser<T> feed(Input input) {
    Result<S, T> result = this.parser.apply(this.state, input);
    if (result.isOk()) {
      return Parser.done(((Ok<S, T>) result).getOutput());
    } else if (result.isCont()) {
      Cont<S, T> cont = (Cont<S, T>) result;
      this.state = cont.getState();
      return this;
    } else if (result.isErr()) {
      return Parser.error(((Err<S, T>) result).getCause());
    } else {
      throw new AssertionError();
    }
  }

}
