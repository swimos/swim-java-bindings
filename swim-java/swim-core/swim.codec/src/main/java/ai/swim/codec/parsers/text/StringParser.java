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

package ai.swim.codec.parsers.text;

import ai.swim.codec.Parser;
import ai.swim.codec.input.Input;

import static ai.swim.codec.parsers.combinators.Chain.chain;
import static ai.swim.codec.parsers.combinators.Peek.peek;
import static ai.swim.codec.parsers.text.EqChar.eqChar;

public class StringParser extends Parser<String> {

  private final StringBuilder output;
  private final int code;
  private int quoteNeedle;
  private Stage stage;
  private UnicodeParser unicodeParser;

  private StringParser(StringBuilder output, int quoteNeedle, int code, Stage stage) {
    this.output = output;
    this.quoteNeedle = quoteNeedle;
    this.code = code;
    this.stage = stage;
  }

  public static Parser<String> stringLiteral() {
    return chain(peek(eqChar('\"')), new StringParser(new StringBuilder(), 0, 0, Stage.Head));
  }

  static boolean isSpace(int c) {
    return c == 0x20 || c == 0x9;
  }

  static boolean isNewline(int c) {
    return c == 0xa || c == 0xd;
  }

  static boolean isWhitespace(int c) {
    return isSpace(c) || isNewline(c);
  }

  public static boolean isDigit(int c) {
    return c >= '0' && c <= '9'
        || c >= 'A' && c <= 'F'
        || c >= 'a' && c <= 'f';
  }

  public static int decodeDigit(int c) {
    if (c >= '0' && c <= '9') {
      return c - '0';
    } else if (c >= 'A' && c <= 'F') {
      return 10 + (c - 'A');
    } else if (c >= 'a' && c <= 'f') {
      return 10 + (c - 'a');
    } else {
      throw new IllegalArgumentException("Invalid base-16 digit: " + c);
    }
  }

  @Override
  public Parser<String> feed(Input input) {
    return parse(input);
  }

  private Parser<String> parse(Input input) {
    int c = 0;

    if (this.stage == Stage.Head) {
      while (input.isContinuation()) {
        c = input.head();
        if (isWhitespace(c)) {
          input = input.step();
        } else {
          break;
        }
      }
      if (input.isContinuation()) {
        if ((c == '"' || c == '\'') && (quoteNeedle == c || quoteNeedle == 0)) {
          input = input.step();
          quoteNeedle = c;
          stage = Stage.Contents;
        } else {
          return error(input, "String quotation mismatch");
        }
      } else if (input.isDone()) {
        return error(input, "Expected a string input");
      }
    }

    do {
      switch (this.stage) {
        case Contents:
          while (input.isContinuation()) {
            c = input.head();
            if (c >= 0x20 && c != quoteNeedle && c != '\\') {
              input = input.step();
              output.appendCodePoint(c);
            } else {
              break;
            }
          }
          if (input.isContinuation()) {
            if (c == quoteNeedle) {
              input.step();
              return done(output.toString());
            } else if (c == '\\') {
              input = input.step();
              stage = Stage.Escaped;
            } else {
              return error(input, "Expected a string input");
            }
          } else if (input.isDone()) {
            return error(input, "Expected a string input");
          } else {
            break;
          }
        case Escaped:
          if (input.isContinuation()) {
            c = input.head();
            if (c == '"' || c == '$' || c == '\'' || c == '/' || c == '@' || c == '[' || c == '\\' || c == ']' || c == '{' || c == '}') {
              output.appendCodePoint(c);
            } else if (c == 'b') {
              output.appendCodePoint('\b');
            } else if (c == 'f') {
              output.appendCodePoint('\f');
            } else if (c == 'n') {
              output.appendCodePoint('\n');
            } else if (c == 'r') {
              output.appendCodePoint('\r');
            } else if (c == 't') {
              output.appendCodePoint('\t');
            } else if (c != 'u') {
              return error(input, "Expected an escape character");
            }
          } else if (input.isDone()) {
            return error(input, "Expected an escape character");
          }

          input = input.step();

          if (c == 'u') {
            stage = Stage.HexDigit;
          } else {
            stage = Stage.Contents;
            continue;
          }
        case HexDigit:
          if (unicodeParser == null) {
            unicodeParser = new UnicodeParser();
          }

          do {
            if (input.isContinuation()) {
              c = input.head();
              if (!unicodeParser.parseUnicodePoint(output, c)) {
                return error(input, "Expected a hex digit");
              } else if (unicodeParser.isDone()) {
                stage = Stage.Contents;
              } else {
                break;
              }
            }
          } while (true);
      }
      break;
    } while (true);

    return new StringParser(output, quoteNeedle, code, stage);
  }

  enum Stage {
    Head,
    Contents,
    Escaped,
    HexDigit,
  }

}
