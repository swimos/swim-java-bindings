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
import static ai.swim.codec.parsers.combinators.Peek.peek;
import static ai.swim.codec.parsers.text.OneOf.oneOf;

public class LineEnding extends Parser<String> {

  private LineEnding() {

  }

  /**
   * Recognizes a line ending character.
   */
  public static Parser<String> lineEnding() {
    return preceded(peek(oneOf('\r', '\n')), new LineEnding());
  }

  @Override
  public Parser<String> feed(Input input) {
    if (input.isContinuation()) {
      char first = (char) input.head();
      if (first == '\n') {
        input.step();
        return Parser.done("\n");
      } else if (first == '\r') {
        input = input.step();
        if (input.isContinuation()) {
          char second = (char) input.head();
          if (second == '\n') {
            input.step();
            return Parser.done("\r\n");
          } else {
            return Parser.error(input, "Expected a line ending");
          }
        } else {
          return this;
        }
      } else {
        return Parser.error(input, "Expected a line ending");
      }
    } else if (input.isEmpty()) {
      return Parser.error(input, "Need more data");
    } else if (input.isDone()) {
      return Parser.error(input, "Invalid input");
    } else {
      return this;
    }
  }
}
