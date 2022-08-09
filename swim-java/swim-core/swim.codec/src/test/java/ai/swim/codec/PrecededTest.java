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

package ai.swim.codec;

import ai.swim.codec.input.Input;
import org.junit.jupiter.api.Test;

import static ai.swim.codec.parsers.text.EqChar.eqChar;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrecededTest {

  public static class Prop extends Parser<String> {
    private final StringBuilder state;

    private Prop() {
      state = new StringBuilder();
    }

    public static Parser<String> parser() {
      return preceded(eqChar('@'), new Prop());
    }

    @Override
    public Parser<String> feed(Input input) {
      while (input.isContinuation()) {
        int head = input.head();
        if (Character.isLetter(head)) {
          state.appendCodePoint(head);
          input = input.step();
        } else {
          return Parser.error(input, "Expected a letter");
        }
      }

      if (input.isDone()) {
        return Parser.done(state.toString());
      } else {
        throw new AssertionError();
      }
    }
  }

  @Test
  void precededTestFin() {
    Parser<String> parser = Prop.parser();
    parser = parser.feed(Input.string("@abc"));

    assertTrue(parser.isDone());
    assertEquals(parser.bind(), "abc");
  }


}