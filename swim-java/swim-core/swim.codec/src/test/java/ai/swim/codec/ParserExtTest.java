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
import static ai.swim.codec.ParserExt.alt;
import static ai.swim.codec.string.StringParser.stringLiteral;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParserExtTest {

  @Test
  void altTestComplete() {
    Parser<String> parser = alt(stringLiteral());

    Parser<String> parseResult = parser.feed(Input.string("\"123\""));
    assertTrue(parseResult.isDone());
    assertEquals(parseResult.bind(), "123");
  }

  @Test
  void altTestCont() {
    Parser<String> parser = alt(stringLiteral());

    Parser<String> parseResult = parser.feed(Input.string("\"abc").isPartial(true));
    assertFalse(parseResult.isError());
    assertTrue(parseResult.isCont());

    parseResult = parseResult.feed(Input.string("def\""));
    assertTrue(parseResult.isDone());
    assertEquals(parseResult.bind(), "abcdef");
  }

  @Test
  void altTestDone() {
    Parser<String> parser = alt(stringLiteral(), Parser.lambda(input -> {
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

  @Test
  void altTestNone() {
    Parser<String> parser = alt(stringLiteral(), new Parser<>() {
      @Override
      public Parser<String> feed(Input input) {
        return this;
      }
    }, Parser.error(""));

    Parser<String> parseResult = parser.feed(Input.string("").isPartial(true));
    assertTrue(parseResult.isCont());
    parseResult = parseResult.feed(Input.string("").isPartial(true));
    parseResult = parseResult.feed(Input.string("\"abc\""));

    assertEquals(parseResult.bind(), "abc");
  }

}