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

package ai.swim.codec.data;

import ai.swim.codec.Parser;
import ai.swim.codec.input.Input;
import org.junit.jupiter.api.Test;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Base64;
import static ai.swim.codec.data.DataParser.blob;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataParserTest {

  void blobTestOk(String input, byte[] expected) {
    Parser<byte[]> parser = blob().feed(Input.string(input));
    assertTrue(parser.isDone());
    assertArrayEquals(parser.bind(), expected);
  }

  @Test
  void parseBlobTest() {
    blobTestOk("%YWI=", "ab".getBytes());
    blobTestOk("%YWJj", "abc".getBytes());
    blobTestOk("%YW55IGNhcm5hbCBwbGVhc3VyZQ==", "any carnal pleasure".getBytes());
    blobTestOk("%YWJj)", "abc".getBytes());
  }

  public static String toHexString(byte[] ba) {
    StringBuilder str = new StringBuilder();
    for (byte b : ba) {
      str.append(String.format("%x", b));
    }
    return str.toString();
  }

}