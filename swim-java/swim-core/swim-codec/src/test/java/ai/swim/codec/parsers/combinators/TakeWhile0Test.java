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
import ai.swim.codec.input.StringInput;
import org.junit.jupiter.api.Test;
import java.util.function.Predicate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TakeWhile0Test {

  @Test
  void takeWhile0Test() {
    Parser<String> parser = new LambdaTakeWhile(Character::isDigit);
    Input input = Input.string("12345abcde");

    parser = parser.feed(input);
    assertTrue(parser.isDone());
    assertEquals(parser.bind(), "12345");

    assertEquals(StringInput.codePointsToString(input.bind()), "abcde");
  }

  private static class LambdaTakeWhile extends TakeWhile0<StringBuilder, String> {

    private final Predicate<Character> predicate;

    public LambdaTakeWhile(Predicate<Character> predicate) {
      super(new StringBuilder());
      this.predicate = predicate;
    }

    @Override
    protected boolean onAdvance(int c, StringBuilder state) {
      if (predicate.test((char) c)) {
        state.appendCodePoint(c);
        return true;
      }

      return false;
    }

    @Override
    protected String onDone(StringBuilder state) {
      return state.toString();
    }
  }
}