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

package ai.swim.codec.parsers;

import ai.swim.codec.Parser;
import ai.swim.codec.parsers.stateful.Result;

import java.util.Base64;

import static ai.swim.codec.Parser.preceded;
import static ai.swim.codec.parsers.StringParsersExt.eqChar;

public final class DataParser {

  private static boolean isDigit(int c) {
    return c >= '0' && c <= '9' || c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z' || c == '+' || c == '/';
  }

  /**
   * Returns a new Base-64 decoder.
   *
   * Base-64 data must be preceded by a {@code %} symbol.
   */
  public static Parser<byte[]> blob() {
    return preceded(eqChar('%'), Parser.stateful(new StringBuilder(), (output, input) -> {
      int c;
      while (input.isContinuation()) {
        c = input.head();

        if (isDigit(c) || c == '=') {
          output.appendCodePoint(c);
          input = input.step();
        } else {
          if (output.length() == 0) {
            return Result.err("Invalid base64 sequence");
          } else {
            return decode(output);
          }
        }
      }

      if (input.isDone()) {
        return decode(output);
      } else {
        return Result.cont(output);
      }
    }));
  }

  private static Result<StringBuilder, byte[]> decode(StringBuilder output) {
    try {
      return Result.ok(Base64.getDecoder().decode(output.toString()));
    } catch (IllegalArgumentException e) {
      return Result.err(e.toString());
    }
  }

}
