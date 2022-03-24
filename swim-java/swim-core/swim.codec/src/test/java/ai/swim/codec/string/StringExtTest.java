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

package ai.swim.codec.string;

import ai.swim.codec.Parser;
import ai.swim.codec.input.Input;
import ai.swim.codec.input.InputError;
import ai.swim.codec.parsers.stateful.Result;
import org.junit.jupiter.api.Test;
import static ai.swim.codec.Parser.preceded;
import static ai.swim.codec.parsers.string.StringExt.multispace0;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StringExtTest {

  private static Parser<String> parser() {
    return preceded(multispace0(), Parser.stateful(new StringBuilder(), (state, input) -> {
      while (input.isContinuation()) {
        int head = input.head();
        if (Character.isDigit(head)) {
          state.appendCodePoint(head);
          input = input.step();
        } else {
          return Result.err("Expected a letter");
        }
      }

      if (input.isDone()) {
        return Result.ok(state.toString());
      } else if (input.isError()) {
        return Result.err(((InputError) input).getCause());
      } else {
        throw new AssertionError();
      }
    }));
  }

  @Test
  void multispace0Test() {
    Parser<String> parser = parser().feed(Input.string("     12345"));
    assertTrue(parser.isDone());
    assertEquals(parser.bind(), "12345");

    parser = parser().feed(Input.string("12345"));
    assertTrue(parser.isDone());
    assertEquals(parser.bind(), "12345");
  }
}