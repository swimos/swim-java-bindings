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

import static ai.swim.codec.parsers.DataParser.blob;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataParserTest {

  void blobTestOk(String input, byte[] expected) {
    Parser<byte[]> parser = blob().feed(Input.string(input));
    assertTrue(parser.isDone());
    assertArrayEquals(parser.bind(), expected);
  }

  void blobTestErr(String input) {
    Parser<byte[]> parser = blob().feed(Input.string(input));
    assertTrue(parser.isError());
  }

  void feedIncremental(String input, byte[] expected) {
    Parser<byte[]> parser = blob();

    for (char c : input.toCharArray()) {
      parser = parser.feed(Input.string(String.valueOf(c)).setPartial(true));
      assertFalse(parser.isDone());
      assertTrue(parser.isCont());
    }

    parser = parser.feed(Input.string(""));
    assertTrue(parser.isDone());

    assertArrayEquals(parser.bind(), expected);
  }

  @Test
  void parseBlobOk() {
    blobTestOk("%YQ==", "a".getBytes());
    blobTestOk("%YWI=", "ab".getBytes());
    blobTestOk("%YWJj", "abc".getBytes());
    blobTestOk("%TG9yZW0gaXBzdW0gZG9sb3Igc2l0IGFtZXQu", "Lorem ipsum dolor sit amet.".getBytes());
    blobTestOk("%YWJj)", "abc".getBytes());
    blobTestOk("%QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODk=", "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".getBytes());
  }

  @Test
  void parseBlobCont() {
    feedIncremental("%YQ==", "a".getBytes());
    feedIncremental("%YWI=", "ab".getBytes());
    feedIncremental("%YWJj", "abc".getBytes());
    feedIncremental("%TG9yZW0gaXBzdW0gZG9sb3Igc2l0IGFtZXQu", "Lorem ipsum dolor sit amet.".getBytes());
    feedIncremental("%QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVphYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5ejAxMjM0NTY3ODk=", "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".getBytes());
  }

  @Test
  void parseBlobErr() {
    blobTestErr("abcd");
    blobTestErr("%!!!!");
  }

}