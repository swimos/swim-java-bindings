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
import ai.swim.codec.ParserError;
import ai.swim.codec.input.Input;
import ai.swim.recon.models.Identifier;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdentifierParserTest {

  void identifierTest(String input, Identifier expected) {
    Parser<Identifier> parser = IdentifierParser.identifier();
    Parser<Identifier> parseResult = parser.feed(Input.string(input));

    assertTrue(parseResult.isDone());
    assertEquals(parseResult.bind(), expected);
  }

  @Test
  void identifierTestTrue() {
    identifierTest("true", Identifier.bool(true));
  }

  @Test
  void identifierTestTrailing() {
    identifierTest("false ", Identifier.bool(false));
  }

  @Test
  void identifierTestFalse() {
    identifierTest("false", Identifier.bool(false));
  }

  @Test
  void identifierTestString() {
    identifierTest("notaboolean", Identifier.string("notaboolean"));
  }

  @Test
  void identifierTestFloat() {
    identifierTest("nan", Identifier.decimal(Float.NaN));

    // todo should these be valid identifiers? - is not a valid start identifier at present.
//    identifierTest("-inf", Identifier.decimal(Float.NEGATIVE_INFINITY));
//    identifierTest("-infinity", Identifier.decimal(Float.NEGATIVE_INFINITY));
    identifierTest("inf", Identifier.decimal(Float.POSITIVE_INFINITY));
    identifierTest("infinity", Identifier.decimal(Float.POSITIVE_INFINITY));
  }

  @Test
  void identifierTestError() {
    Parser<Identifier> parser = IdentifierParser.identifier();
    Parser<Identifier> parseResult = parser.feed(Input.string("@"));

    assertTrue(parseResult.isError());
    assertEquals(((ParserError<Identifier>) parseResult).getCause(), "Expected an identifier");
  }

  @Test
  void identifierTestContinuation() {
    Parser<Identifier> parser = IdentifierParser.identifier();
    Parser<Identifier> parseResult = parser.feed(Input.string("tr").isPartial(true));

    assertTrue(parseResult.isCont());
    parseResult = parseResult.feed(Input.string("ue"));
    assertTrue(parseResult.isDone());
    assertEquals(parseResult.bind(), Identifier.bool(true));
  }
}