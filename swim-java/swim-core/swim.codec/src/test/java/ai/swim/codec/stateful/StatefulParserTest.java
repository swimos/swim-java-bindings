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

package ai.swim.codec.stateful;

import ai.swim.codec.Parser;
import ai.swim.codec.ParserError;
import ai.swim.codec.input.Input;
import ai.swim.codec.parsers.stateful.Result;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatefulParserTest {

  @Test
  void parseOk() {
    Parser<Integer> parser = Parser.stateful(new Object(), (o, input) -> Result.ok(1)).feed(Input.string(""));
    assertTrue(parser.isDone());
    assertEquals(parser.bind(), 1);
  }

  @Test
  void parseCont() {
    Parser<String> parser = Parser.stateful(0, (state, input) -> {
      if (state == 5) {
        return Result.ok("done");
      } else {
        state += 1;
        return Result.cont(state);
      }
    });

    for (int i = 0; i < 5; i++) {
      parser = parser.feed(Input.string(""));
      assertFalse(parser.isDone());
    }

    parser = parser.feed(Input.string(""));
    assertTrue(parser.isDone());
    assertEquals(parser.bind(), "done");
  }

  @Test
  void parseErr() {
    Parser<Object> parser = Parser.stateful(0, (s, input) -> Result.err("Err")).feed(Input.string(""));
    assertTrue(parser.isError());
    assertEquals(((ParserError<?>) parser).getCause(), "Err");
  }

}