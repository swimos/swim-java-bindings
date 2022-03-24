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
import ai.swim.recon.models.ParserTransition;
import ai.swim.recon.models.events.ParseEvents;
import ai.swim.recon.models.state.ChangeState;
import ai.swim.recon.models.state.PushAttr;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReconParserTest {

  void initTestOk(String input, ParserTransition expected) {
    Parser<ParserTransition> parser = ReconParser.parserInit().feed(Input.string(input));
    assertTrue(parser.isDone());

    assertEquals(expected, parser.bind());
  }

  @Test
  void initTest() {
    initTestOk("\"hello\"", new ParserTransition(ReadEvent.text("hello"), null));
    initTestOk("true", new ParserTransition(ReadEvent.bool(true), null));
    initTestOk("12345.0", new ParserTransition(ReadEvent.number(12345.0f), null));
  }

  @Test
  void initTestCont() {
    Parser<ParserTransition> parser = ReconParser.parserInit();
    Parser<ParserTransition> parseResult = parser.feed(Input.string("\"hi").isPartial(true));

    assertTrue(parseResult.isCont());

    parseResult = parseResult.feed(Input.string(" there\""));
    assertTrue(parseResult.isDone());
    assertEquals(parseResult.bind(), new ParserTransition(ReadEvent.text("hi there"), null));
  }

  void attrOkDoneTest(String input, ParserTransition expected) {
    Parser<ParserTransition> parser = ReconParser.parserInit();
    parser = parser.feed(Input.string(input));

    assertTrue(parser.isDone());
    assertEquals(expected, parser.bind());
  }

  @Test
  void attrsDone() {
    attrOkDoneTest("@attrName()", new ParserTransition(ReadEvent.startAttribute("attrName"), new PushAttr()));
    attrOkDoneTest("@attrName{}", new ParserTransition(ReadEvent.startAttribute("attrName"), ReadEvent.endAttribute(), new ChangeState(ParseEvents.ParseState.AfterAttr)));
    attrOkDoneTest("@attrName", new ParserTransition(ReadEvent.startAttribute("attrName"), ReadEvent.endAttribute(), new ChangeState(ParseEvents.ParseState.AfterAttr)));
  }

  void attrsContTest(String input, ParserTransition expected) {
    Parser<ParserTransition> parser = ReconParser.parserInit();
    char[] chars = input.toCharArray();

    for (int i = 0; i < input.length(); i++) {
      char c = chars[i];
      boolean isPartial = i + 1 != input.length();
      parser = parser.feed(Input.string(String.valueOf(c)).isPartial(isPartial));

      if (isPartial) {
        assertFalse(parser.isDone());
        assertTrue(parser.isCont());
      } else {
        assertTrue(parser.isDone());
        assertFalse(parser.isCont());
      }
    }

    assertEquals(parser.bind(), expected);
  }

  @Test
  void attrsCont() {
    attrsContTest("@attrName(", new ParserTransition(ReadEvent.startAttribute("attrName"), new PushAttr()));
    attrsContTest("@attrName{", new ParserTransition(ReadEvent.startAttribute("attrName"), ReadEvent.endAttribute(), new ChangeState(ParseEvents.ParseState.AfterAttr)));
    attrsContTest("@attrName", new ParserTransition(ReadEvent.startAttribute("attrName"), ReadEvent.endAttribute(), new ChangeState(ParseEvents.ParseState.AfterAttr)));
  }

}