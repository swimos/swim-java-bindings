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
import static ai.swim.codec.Parser.preceded;
import static ai.swim.codec.parsers.OptParser.opt;
import static ai.swim.codec.parsers.number.NumberParser.numericLiteral;
import static ai.swim.codec.parsers.string.EqChar.eqChar;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OptParserTest {

  @Test
  void testOptMatch() {
    Parser<String> parser = preceded(opt(eqChar('@')), numericLiteral().map(String::valueOf));
    parser = parser.feed(Input.string("@1234"));

    assertTrue(parser.isDone());
    assertEquals(parser.bind(), "1234");
  }

  @Test
  void testOptNoMatch() {
    Parser<String> parser = preceded(opt(eqChar('@')), numericLiteral().map(String::valueOf));
    parser = parser.feed(Input.string("1234"));

    assertTrue(parser.isDone());
    assertEquals(parser.bind(), "1234");
  }

  @Test
  void testOptCont() {
    Parser<String> parser = preceded(opt(eqChar('@')), numericLiteral().map(String::valueOf));

    parser = parser.feed(Input.string("").isPartial(true));
    assertTrue(parser.isCont());

    parser = parser.feed(Input.string("@").isPartial(true));
    assertTrue(parser.isCont());

    parser = parser.feed(Input.string("1234").isPartial(true));
    assertTrue(parser.isCont());

    parser = parser.feed(Input.string("5"));
    assertTrue(parser.isDone());
    assertEquals(parser.bind(), "12345");
  }

}