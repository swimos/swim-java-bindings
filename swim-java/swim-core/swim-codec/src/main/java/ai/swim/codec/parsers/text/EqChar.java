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

package ai.swim.codec.parsers.text;

import ai.swim.codec.Parser;
import ai.swim.codec.input.Input;

public class EqChar extends Parser<Character> {
  private final char c;

  private EqChar(char c) {
    this.c = c;
  }

  public static Parser<Character> eqChar(char c) {
    return new EqChar(c);
  }

  @Override
  public Parser<Character> feed(Input input) {
    if (input.isContinuation()) {
      char head = (char) input.head();
      if (head == c) {
        input.step();
        return Parser.done(head);
      } else {
        return Parser.error(input, String.format("Expected '%s', found '%s'", c, head));
      }
    } else if (input.isDone()) {
      return Parser.error(input, "Insufficient data");
    } else {
      return this;
    }
  }
}
