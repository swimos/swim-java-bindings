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

package ai.swim.recon;

import ai.swim.codec.Parser;
import ai.swim.codec.input.Input;
import ai.swim.recon.event.ReadEvent;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReconParserTest {

  void initTestOk(String input, ReadEvent expected) {
    Parser<ReadEvent> parser = ReconParser.init().feed(Input.string(input));
    assertTrue(parser.isDone());

    assertEquals(expected, parser.bind());
  }

  @Test
  void initTest() {
    initTestOk("\"hello\"", ReadEvent.text("hello"));
    initTestOk("true", ReadEvent.bool(true));
    initTestOk("12345.0", ReadEvent.number(12345.0f));
  }

  @Test
  void initTestCont() {
    Parser<ReadEvent> parser = ReconParser.init();
    Parser<ReadEvent> parseResult = parser.feed(Input.string("\"hi").isPartial(true));

    assertTrue(parseResult.isCont());

    parseResult = parseResult.feed(Input.string(" there\""));
    assertTrue(parseResult.isDone());
    assertEquals(parseResult.bind(), ReadEvent.text("hi there"));
  }

}