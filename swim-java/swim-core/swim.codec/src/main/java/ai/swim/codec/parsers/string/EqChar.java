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

package ai.swim.codec.parsers.string;

import ai.swim.codec.Parser;
import ai.swim.codec.input.Input;
import ai.swim.codec.input.InputError;

public class EqChar extends Parser<Character> {

  private final int c;

  public EqChar(int c) {
    this.c = c;
  }

  public static EqChar eqChar(int c) {
    return new EqChar(c);
  }

  @Override
  public Parser<Character> feed(Input input) {
    if (input.isContinuation()) {
      int head = input.head();
      input.step();
      if (head == this.c) {
        return Parser.done((char) head);
      } else {
        return Parser.error("Expected: " + head);
      }
    } else if (input.isError()) {
      return Parser.error(((InputError) input).getCause());
    } else if (input.isDone()) {
      return Parser.error("Insufficient data");
    } else {
      return this;
    }

  }
}
