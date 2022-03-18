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
import java.util.Base64;

final class DataParser extends Parser<byte[]> {

  private final boolean readHead;
  private final boolean fin;
  private final int idx;
  private final StringBuilder output;

  public DataParser(StringBuilder output, boolean readHead, boolean fin, int idx) {
    this.output = output;
    this.readHead = readHead;
    this.fin = fin;
    this.idx = idx;
  }

  private static boolean isDigit(int c) {
    return c >= '0' && c <= '9' || c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z' || c == '+' || c == '/';
  }

  public static Parser<byte[]> blob() {
    return new DataParser(null, false, false, 1);
  }

  static Parser<byte[]> parse(Input input, StringBuilder output, boolean readHead, boolean fin, int idx) {
    int c;

    if (!readHead) {
      if (input.isContinuation()) {
        c = input.head();

        if (c == '%') {
          readHead = true;
          input = input.step();
          c = input.head();
          output = new StringBuilder();
        } else {
          error("Expected: %");
        }
      }
    }

    while (input.isContinuation()) {
      c = input.head();


      if (!isDigit(c) && c != '=') {
        return decode(output);
      }

      if (idx < 3) {
        if (isDigit(c)) {
          output.appendCodePoint(c);
        } else {
          return error("Expected a base64 character");
        }
      } else if (idx == 3) {
        if (isDigit(c)) {
          output.appendCodePoint(c);
        } else {
          fin = true;
          output.appendCodePoint(c);
        }
      } else if (idx == 4) {
        output.appendCodePoint(c);
      } else {
        throw new AssertionError();
      }

      input = input.step();

      if (input.isDone()) {
        return decode(output);
      }

      idx += 1;
      if (idx > 4) {
        idx = 1;
      }
    }

    return new DataParser(output, readHead, fin, idx);
  }

  private static Parser<byte[]> decode(StringBuilder output) {
    try {
      return Parser.done(Base64.getDecoder().decode(output.toString()));
    } catch (IllegalArgumentException e) {
      return Parser.error(e.toString());
    }
  }

  public static int decodeDigit(int c) {
    if (c >= 'A' && c <= 'Z') {
      return c - 'A';
    } else if (c >= 'a' && c <= 'z') {
      return c + (26 - 'a');
    } else if (c >= '0' && c <= '9') {
      return c + (52 - '0');
    } else if (c == '+' || c == '-') {
      return 62;
    } else if (c == '/' || c == '_') {
      return 63;
    } else {
      throw new IllegalArgumentException("Invalid base-64 digit: " + c);
    }
  }

  private static void writeQuantum(int c1, int c2, int c3, int c4, ByteArrayOutput output) {
    final int x = decodeDigit(c1);
    final int y = decodeDigit(c2);
    if (c3 != '=') {
      final int z = decodeDigit(c3);
      if (c4 != '=') {
        final int w = decodeDigit(c4);
        output.push((x << 2) | (y >>> 4));
        output.push((y << 4) | (z >>> 2));
        output.push((z << 6) | w);
      } else {
        output.push((x << 2) | (y >>> 4));
        output.push((y << 4) | (z >>> 2));
      }
    } else {
      if (c4 != '=') {
        throw new IllegalArgumentException("Improperly padded base-64");
      }
      output.push((x << 2) | (y >>> 4));
    }
  }


  @Override
  public Parser<byte[]> feed(Input input) {
    return parse(input, this.output, this.readHead, this.fin, this.idx);
  }

}
