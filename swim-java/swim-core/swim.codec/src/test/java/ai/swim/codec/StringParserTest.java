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
import ai.swim.codec.parsers.string.StringParser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StringParserTest {

  @Test
  public void t() {
    Parser<ReadEvent> parser = StringParser.stringLiteral().map(StringReadEvent::new);
    Parser<ReadEvent> parseResult = parser.feed(Input.string("\"abcdef\""));
    System.out.println(parseResult.isError());
    System.out.println(parseResult.bind());
  }

  @Test
  public void t2() {
    Parser<ReadEvent> parser = StringParser.stringLiteral().map(StringReadEvent::new);
    Parser<ReadEvent> parseResult = parser.feed(Input.string("\"abc").isPartial(true));
    assertFalse(parseResult.isDone());
    assertFalse(parseResult.isError());

    parseResult = parser.feed(Input.string("def\""));
    assertTrue(parseResult.isDone());
    assertEquals(((StringReadEvent) parseResult.bind()).value, "abcdef");
  }

  @Test
  void escapes() {
    Parser<String> parser = StringParser.stringLiteral();
    Parser<String> parseResult = parser.feed(Input.string("\"a\\nmulti\\nline\\t\\ninput\""));
    assertTrue(parseResult.isDone());
    assertEquals(parseResult.bind(), "a\nmulti\nline\t\ninput");
  }

  abstract class ReadEvent {

  }

  class StringReadEvent extends ReadEvent {
    String value;

    StringReadEvent(String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return "StringReadEvent{" +
          "value='" + value + '\'' +
          '}';
    }
  }
}