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

package ai.swim.codec.parsers.number;

import ai.swim.codec.Parser;
import ai.swim.codec.input.Input;

import java.math.BigDecimal;
import java.math.BigInteger;

import static ai.swim.codec.parsers.ParserExt.peek;
import static ai.swim.codec.parsers.StringParsersExt.oneOf;

public final class NumberParser extends Parser<Number> {

  private final boolean isNegative;
  private final long value;
  private final Stage stage;
  private final StringBuilder floatLiteralBuilder;

  NumberParser(boolean isNegative, long value, Stage stage, StringBuilder floatLiteralBuilder) {
    this.isNegative = isNegative;
    this.value = value;
    this.stage = stage;
    this.floatLiteralBuilder = floatLiteralBuilder;
  }

  static Parser<Number> parse(Input input, boolean isNegative, long value, Stage stage, StringBuilder floatLiteralBuilder) {
    int c;
    if (stage == Stage.Sign) {
      if (input.isContinuation()) {
        c = input.head();
        if (c == '-') {
          input = input.step();
          isNegative = true;
        }
        stage = Stage.Alt;
      } else if (input.isDone()) {
        return error(input, "Expected a number");
      }
    }

    int sign = isNegative ? -1 : 1;

    do {
      if (stage == Stage.Alt) {
        if (input.isContinuation()) {
          c = input.head();
          if (c == '0') {
            input = input.step();
            stage = Stage.Decimal;
          } else if (c >= '1' && c <= '9') {
            input = input.step();
            value = (long) sign * (c - '0');
            stage = Stage.Integer;
          } else if (c == '.') {
            stage = Stage.Decimal;
          } else if (isFloatChar(c)) {
            if (floatLiteralBuilder == null) {
              floatLiteralBuilder = new StringBuilder();
            }

            floatLiteralBuilder.appendCodePoint(c);
            input = input.step();
            continue;
          } else {
            if (floatLiteralBuilder != null) {
              return parseLiteralFloat(floatLiteralBuilder, isNegative, input);
            }

            return error(input, "Expected a number");
          }
        }
      }
      if (stage == Stage.Integer) {
        while (input.isContinuation()) {
          c = input.head();
          if (c >= '0' && c <= '9') {
            final long newValue = 10 * value + (long) sign * (c - '0');
            if (newValue / value >= 10) {
              value = newValue;
              input = input.step();
            } else {
              return BigIntegerParser.parse(input, sign, BigInteger.valueOf(value));
            }
          } else {
            break;
          }
        }
        if (input.isContinuation()) {
          stage = Stage.Decimal;
        } else if (input.isDone()) {
          return done(valueOf(value));
        }
      }

      if (stage == Stage.Decimal) {
        if (input.isContinuation()) {
          c = input.head();
          if (c == '.' || (c == 'E' || c == 'e')) {
            return DecimalParser.parse(input, sign, value);
          } else if (c == 'x' && !isNegative && value == 0L) {
            input = input.step();
            return HexadecimalParser.parse(input);
          } else if (c >= '0' && c <= '9') {
            stage = Stage.Alt;
            continue;
          } else {
            return done(valueOf(value));
          }
        } else if (input.isDone()) {
          return done(valueOf(value));
        }
      }
      break;
    } while (input.isContinuation());

    if (input.isError()) {
      return error(input, "Expected a number");
    }

    if (input.isDone() && floatLiteralBuilder != null) {
      return parseLiteralFloat(floatLiteralBuilder, isNegative, input);
    }

    return new NumberParser(isNegative, value, stage, floatLiteralBuilder);
  }

  private static Parser<Number> parseLiteralFloat(StringBuilder floatLiteralBuilder, boolean isNegative, Input input) {
    String fs = floatLiteralBuilder.toString();
    if ("nan".equalsIgnoreCase(fs) && !isNegative) {
      return Parser.done(Float.NaN);
    } else if ("inf".equalsIgnoreCase(fs) || "infinity".equalsIgnoreCase(fs)) {
      if (isNegative) {
        return Parser.done(Float.NEGATIVE_INFINITY);
      } else {
        return Parser.done(Float.POSITIVE_INFINITY);
      }
    } else {
      return error(input, "Expected a number");
    }
  }

  public static Parser<Number> numericLiteral() {
    return preceded(
        peek(oneOf('-', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '.')),
        new NumberParser(false, 0, Stage.Sign, null)
    );
  }

  public static boolean isFloatChar(int c) {
    return c == 'N' || c == 'n' || c == 'A' || c == 'a' || c == 'I' || c == 'i' || c == 'F' || c == 'f' || c == 'T' || c == 't' || c == 'Y' || c == 'y';
  }

  public static Number valueOf(long value) {
    if ((long) (int) value == value) {
      return (int) value;
    } else {
      return value;
    }
  }

  public static Number valueOf(String value) {
    try {
      final long longValue = Long.parseLong(value);
      if ((long) (int) longValue == longValue) {
        return (int) longValue;
      } else {
        return longValue;
      }
    } catch (NumberFormatException e1) {
      try {
        final double doubleValue = Double.parseDouble(value);
        if ((double) (float) doubleValue == doubleValue) {
          return (float) doubleValue;
        } else {
          return doubleValue;
        }
      } catch (NumberFormatException e2) {
        return new BigDecimal(value);
      }
    }
  }

  @Override
  public Parser<Number> feed(Input input) {
    return parse(input, this.isNegative, this.value, this.stage, this.floatLiteralBuilder);
  }

  enum Stage {
    Sign,
    Alt,
    Integer,
    Decimal,
  }

}

