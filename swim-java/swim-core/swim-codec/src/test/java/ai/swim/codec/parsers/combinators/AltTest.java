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
import org.junit.jupiter.api.Test;
import static ai.swim.codec.LambdaParser.lambda;
import static ai.swim.codec.parsers.combinators.Alt.alt;
import static ai.swim.codec.parsers.text.StringParser.stringLiteral;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AltTest {
  @Test
  void altTestComplete() {
    Parser<String> parser = alt(stringLiteral());

    Parser<String> parseResult = parser.feed(Input.string("\"123\""));
    assertTrue(parseResult.isDone());
    assertEquals(parseResult.bind(), "123");
  }

  @Test
  void altTestCont() {
    Parser<String> parser = alt(stringLiteral()).feed(Input.string("\"abc").setPartial(true));
    assertFalse(parser.isError());
    assertTrue(parser.isCont());

    parser = parser.feed(Input.string("def\""));
    assertTrue(parser.isDone());
    assertEquals(parser.bind(), "abcdef");
  }

  @Test
  void altTestDone() {
    Parser<String> parser = alt(stringLiteral(), lambda(input -> {
      StringBuilder sb = new StringBuilder();
      while (input.isContinuation()) {
        sb.appendCodePoint(input.head());
        input = input.step();
      }

      return Parser.done(sb.toString());
    }));

    Parser<String> parseResult = parser.feed(Input.string("abcdef123"));
    assertTrue(parseResult.isDone());
    assertEquals(parseResult.bind(), "abcdef123");
  }
}