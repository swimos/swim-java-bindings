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
import ai.swim.recon.models.ParseState;
import ai.swim.recon.models.ParserTransition;
import ai.swim.recon.models.items.ItemsKind;
import ai.swim.recon.models.state.Action;
import ai.swim.recon.models.state.ParseEvent;
import ai.swim.recon.models.state.PushAttrNewRec;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReconParserPartsTest {

  void initTestOk(String input, ParserTransition expected) {
    Parser<ParserTransition> parser = ReconParserParts.parseInit().feed(Input.string(input));
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
    Parser<ParserTransition> parser = ReconParserParts.parseInit();
    Parser<ParserTransition> parseResult = parser.feed(Input.string("\"hi").setPartial(true));

    assertTrue(parseResult.isCont());

    parseResult = parseResult.feed(Input.string(" there\""));
    assertTrue(parseResult.isDone());
    assertEquals(parseResult.bind(), new ParserTransition(ReadEvent.text("hi there"), null));
  }

  void attrOkDoneTest(String input, ParserTransition expected) {
    Parser<ParserTransition> parser = ReconParserParts.parseInit();
    parser = parser.feed(Input.string(input));

    assertTrue(parser.isDone());
    assertEquals(expected, parser.bind());
  }

  @Test
  void attrsDone() {
    attrOkDoneTest("@attrName()", new ParserTransition(ReadEvent.startAttribute("attrName"), Action.pushAttr()));
    attrOkDoneTest(
        "@attrName{}",
        new ParserTransition(
            ReadEvent.startAttribute("attrName"),
            ReadEvent.endAttribute(),
            new ParseEvent(ParseState.AfterAttr)));
    attrOkDoneTest(
        "@attrName",
        new ParserTransition(
            ReadEvent.startAttribute("attrName"),
            ReadEvent.endAttribute(),
            new ParseEvent(ParseState.AfterAttr)));
  }

  void attrsContTest(String input, ParserTransition expected) {
    Parser<ParserTransition> parser = ReconParserParts.parseInit();
    char[] chars = input.toCharArray();

    for (int i = 0; i < input.length(); i++) {
      char c = chars[i];
      boolean isPartial = i + 1 != input.length();
      parser = parser.feed(Input.string(String.valueOf(c)).setPartial(isPartial));

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
    attrsContTest("@attrName(", new ParserTransition(ReadEvent.startAttribute("attrName"), Action.pushAttr()));
    attrsContTest(
        "@attrName{",
        new ParserTransition(
            ReadEvent.startAttribute("attrName"),
            ReadEvent.endAttribute(),
            new ParseEvent(ParseState.AfterAttr)));
    attrsContTest(
        "@attrName",
        new ParserTransition(
            ReadEvent.startAttribute("attrName"),
            ReadEvent.endAttribute(),
            new ParseEvent(ParseState.AfterAttr)));
  }

  @Test
  void parseAfterAttrTest() {
    Parser<ParserTransition> parser = ReconParserParts.parseAfterAttr();
    parser = parser.feed(Input.string(")"));
    assertTrue(parser.isDone());
    assertEquals(
        parser.bind(),
        new ParserTransition(ReadEvent.startBody(), ReadEvent.endRecord(), Action.popAfterItem()));
  }

  void parseNotAfterItemExec(String input, ParserTransition expected) {
    Parser<ParserTransition> parser = ReconParserParts.parseNotAfterItem(ItemsKind.record(), false);
    parser = parser.feed(Input.string(input));

    assertTrue(parser.isDone());
    assertEquals(expected, parser.bind());
  }

  @Test
  void parseNotAfterItemTest() {
    parseNotAfterItemExec(
        ":1, b:2)3",
        new ParserTransition(
            ReadEvent.extant(),
            ReadEvent.slot(),
            new ParseEvent(ParseState.RecordBodySlot)));
    parseNotAfterItemExec("}", new ParserTransition(ReadEvent.endRecord(), Action.popAfterItem()));
    parseNotAfterItemExec(
        "\"abc\"",
        new ParserTransition(ReadEvent.text("abc"), new ParseEvent(ParseState.RecordBodyAfterValue)));
    parseNotAfterItemExec(
        "@inner()",
        new ParserTransition(ReadEvent.startAttribute("inner"), new PushAttrNewRec(true)));
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  void parseAfterSlotExec(String input, Optional<ParseState> expected) {
    Parser<Optional<ParseState>> parser = ReconParserParts.parseAfterSlot(ItemsKind.attr());
    parser = parser.feed(Input.string(input));

    assertTrue(parser.isDone());
    assertEquals(expected, parser.bind());
  }

  @Test
  void parseAfterSlotTest() {
    parseAfterSlotExec(", b:2)3", Optional.of(ParseState.AttrBodyAfterSep));
    parseAfterSlotExec("\r\n)3", Optional.of(ParseState.AttrBodyStartOrNl));
    parseAfterSlotExec(")3", Optional.empty());
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  void parseAfterValueExec(String input, Optional<ParseState> expected) {
    Parser<Optional<ParseState>> parser = ReconParserParts.parseAfterValue(ItemsKind.attr());
    parser = parser.feed(Input.string(input));

    assertTrue(parser.isDone());
    assertEquals(expected, parser.bind());
  }

  @Test
  void parseAfterValueTest() {
    parseAfterValueExec(":1, b:2)3", Optional.of(ParseState.AttrBodySlot));
    parseAfterValueExec(":2)3", Optional.of(ParseState.AttrBodySlot));
  }

  void parseSlotValueExec(String input, ParserTransition expected) {
    Parser<ParserTransition> parser = ReconParserParts.parseSlotValue(ItemsKind.attr());
    parser = parser.feed(Input.string(input));

    assertTrue(parser.isDone());
    assertEquals(expected, parser.bind());
  }

  @Test
  void parseSlotValueTest() {
    parseSlotValueExec(
        "1, b:2)3",
        new ParserTransition(ReadEvent.number(1), new ParseEvent(ParseState.AttrBodyAfterSlot)));
    parseSlotValueExec("2)3", new ParserTransition(ReadEvent.number(2), new ParseEvent(ParseState.AttrBodyAfterSlot)));
    parseSlotValueExec(
        "abcd, c:4)",
        new ParserTransition(ReadEvent.text("abcd"), new ParseEvent(ParseState.AttrBodyAfterSlot)));
    parseSlotValueExec("{e:4, f:5})", new ParserTransition(ReadEvent.startBody(), Action.pushBody()));
  }

}