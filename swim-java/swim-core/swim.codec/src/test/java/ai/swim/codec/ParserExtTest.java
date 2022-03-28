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
import ai.swim.codec.input.StringInput;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static ai.swim.codec.parsers.StringParsersExt.eqChar;
import static ai.swim.codec.parsers.MapReduce.mapReduce;
import static ai.swim.codec.parsers.ParserExt.*;
import static ai.swim.codec.parsers.string.StringParser.stringLiteral;
import static org.junit.jupiter.api.Assertions.*;

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

  @Test
  void takeWhile0Test() {
    Parser<String> parser = takeWhile0(Character::isDigit);
    Input input = Input.string("12345abcde");

    parser = parser.feed(input);
    assertTrue(parser.isDone());
    assertEquals(parser.bind(), "12345");

    assertEquals(StringInput.codePointsToString(input.bind()), "abcde");
  }

  void many0Ok(String input, String expectedChars) {
    Parser<List<Character>> parser = many0(eqChar('a'));
    parser = parser.feed(Input.string(input));
    assertTrue(parser.isDone());
    assertEquals(parser.bind(), expectedChars.chars().mapToObj(c -> (char) c).collect(Collectors.toList()));
  }

  @Test
  void many0TestOk() {
    many0Ok("aaaabcde", "aaaa");
    many0Ok("a", "a");
    many0Ok("", "");
    many0Ok(" a", "");
    many0Ok("bcd", "");

    Parser<String> parser = mapReduce(many0(eqChar('a'))).feed(Input.string("aaaabcd"));
    assertTrue(parser.isDone());
    assertEquals(parser.bind(), "aaaa");
  }

  void many0Cont(String input, String expectedChars) {
    Parser<List<Character>> parser = many0(eqChar('a'));

    for (char c : input.toCharArray()) {
      parser = parser.feed(Input.string(String.valueOf(c)).isPartial(true));

      if (c == 'a') {
        assertFalse(parser.isDone());
        assertFalse(parser.isError());
      } else {
        assertTrue(parser.isDone());
        break;
      }
    }

    if (!parser.isDone()) {
      parser = parser.feed(Input.string(""));
    }

    assertTrue(parser.isDone());
    assertEquals(parser.bind(), expectedChars.chars().mapToObj(c -> (char) c).collect(Collectors.toList()));
  }

  @Test
  void many0TestCont() {
    many0Cont("aaaabcde", "aaaa");
    many0Cont("a", "a");
    many0Cont("", "");
    many0Cont(" a", "");
    many0Cont("bcd", "");
  }

  @Test
  void many0TestErr() {
    Parser<List<Character>> parser = many0(eqChar('a').andThen(c -> Parser.error("err")));
    parser = parser.feed(Input.string("abc"));
    assertTrue(parser.isDone());
    assertEquals(parser.bind(), Collections.emptyList());

    Parser<List<Character>> p2 = many0(Parser.lambda(input -> {
      char c = (char) input.head();
      if (c != 'a') {
        return Parser.error(String.valueOf(c));
      } else {
        input.step();
        return Parser.done(c);
      }
    }));

    p2 = p2.feed(Input.string("aabcd"));
    assertTrue(p2.isDone());
    assertEquals(p2.bind(), List.of('a', 'a'));
  }
}