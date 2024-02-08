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

public abstract class TakeWhile0<S, T> extends Parser<T> {

  private final S state;

  protected TakeWhile0(S state) {
    this.state = state;
  }

  @Override
  public Parser<T> feed(Input input) {
    while (true) {
      if (input.isDone()) {
        return Parser.done(onDone(state));
      } else if (input.isContinuation()) {
        int c = input.head();

        if (onAdvance(c, state)) {
          input = input.step();
        } else {
          return Parser.done(onDone(state));
        }
      } else {
        return this;
      }
    }
  }

  protected abstract boolean onAdvance(int c, S state);

  protected abstract T onDone(S state);
}
