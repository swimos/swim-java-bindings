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

final class DataParser extends Parser<byte[]> {

  final ByteArrayOutput output;
  final int p;
  final int q;
  final int r;
  final int step;

  DataParser(ByteArrayOutput output, int p, int q, int r, int step) {
    this.output = output;
    this.p = p;
    this.q = q;
    this.r = r;
    this.step = step;
  }

  DataParser(ByteArrayOutput output) {
    this(output, 0, 0, 0, 0);
  }

  private static boolean isDigit(int c) {
    return c >= '0' && c <= '9'
        || c >= 'A' && c <= 'Z'
        || c >= 'a' && c <= 'z'
        || c == '+' || c == '/';
  }

  public static Parser<byte[]> blob() {
    return new DataParser(new ByteArrayOutput(32));
  }

  static Parser<byte[]> parse(Input input, ByteArrayOutput output, int p, int q, int r, int step) {
    int c = 0;

    if (step == 0) {
      if (input.isContinuation()) {
        c = input.head();
        if (c == '%') {
          input = input.step();
          step = 1;
        } else {
          return error("Expected a blob");
        }
      } else if (input.isDone()) {
        return error("Expected a blob");
      }
    }

    while (!input.isError() && !input.isEmpty()) {
      if (step == 1) {
        if (input.isContinuation()) {
          c = input.head();
          if (isDigit(c)) {
            input = input.step();
            p = c;
            step = 2;
          } else {
            return done(output.getData());
          }
        } else if (input.isDone()) {
          return done(output.getData());
        }
      }
      if (step == 2) {
        if (input.isContinuation()) {
          c = input.head();
          if (isDigit(c)) {
            input = input.step();
            q = c;
            step = 3;
          } else {
            return error("Expected a blob");
          }
        } else if (input.isDone()) {
          return error("Expected a blob");
        }
      }
      if (step == 3) {
        if (input.isContinuation()) {
          c = input.head();
          if (isDigit(c) || c == '=') {
            input = input.step();
            r = c;
            if (c != '=') {
              step = 4;
            } else {
              step = 5;
            }
          } else {
            return error("Expected a blob");
          }
        } else if (input.isDone()) {
          return error("Expected a blob");
        }
      }
      if (step == 4) {
        if (input.isContinuation()) {
          c = input.head();
          if (isDigit(c) || c == '=') {
            input = input.step();
            writeQuantum(p, q, r, c, output);
            r = 0;
            q = 0;
            p = 0;
            if (c != '=') {
              step = 1;
            } else {
              return done(output.getData());
            }
          } else {
            return error("Expected a blob");
          }
        } else if (input.isDone()) {
          return error("Expected a blob");
        }
      } else if (step == 5) {
        if (input.isContinuation()) {
          c = input.head();
          if (c == '=') {
            input = input.step();
            writeQuantum(p, q, r, c, output);
            r = 0;
            q = 0;
            p = 0;
            return done(output.getData());
          } else {
            return error("Expected a blob");
          }
        } else if (input.isDone()) {
          return error("Expected a blob");
        }
      }
    }
    if (input.isError()) {
      return error("Expected a blob");
    }
    return new DataParser(output, p, q, r, step);
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
    return parse(input, this.output, this.p, this.q, this.r, this.step);
  }

}
