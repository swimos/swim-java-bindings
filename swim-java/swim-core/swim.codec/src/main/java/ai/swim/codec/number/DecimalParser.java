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

package ai.swim.codec.number;

import ai.swim.codec.Parser;
import ai.swim.codec.input.Input;

final class DecimalParser extends Parser<Number> {

  final StringBuilder builder;
  final int step;

  DecimalParser(StringBuilder builder,  int step) {
    this.builder = builder;
    this.step = step;
  }

  static Parser<Number> parse(Input input, StringBuilder builder, int step) {
    int c;
    if (step == 1) {
      if (input.isContinuation()) {
        c = input.head();
        if (c == '.') {
          input = input.step();
          builder.appendCodePoint(c);
          step = 2;
        } else if (c == 'E' || c == 'e') {
          input = input.step();
          builder.appendCodePoint(c);
          step = 5;
        } else {
          return error("Expected a decimal or exponent");
        }
      } else if (input.isDone()) {
        return error("Expected a decimal or exponent");
      }
    }
    if (step == 2) {
      if (input.isContinuation()) {
        c = input.head();
        if (c >= '0' && c <= '9') {
          input = input.step();
          builder.appendCodePoint(c);
          step = 3;
        } else if(c=='.'){
          return error("Expected a digit");
        }else {
          return done(NumberParser.valueOf(builder.toString()));
        }
      } else if (input.isDone()) {
        return error("Expected a digit");
      }
    }
    if (step == 3) {
      while (input.isContinuation()) {
        c = input.head();
        if (c >= '0' && c <= '9') {
          input = input.step();
          builder.appendCodePoint(c);
        } else {
          break;
        }
      }
      if (input.isContinuation()) {
          step = 4;
      } else if (input.isDone()) {
        return done(NumberParser.valueOf(builder.toString()));
      }
    }
    if (step == 4) {
      c = input.head();
      if (c == 'E' || c == 'e') {
        input = input.step();
        builder.appendCodePoint(c);
        step = 5;
      } else {
        return done(NumberParser.valueOf(builder.toString()));
      }
    }
    if (step == 5) {
      if (input.isContinuation()) {
        c = input.head();
        if (c == '+' || c == '-') {
          input = input.step();
          builder.appendCodePoint(c);
        }
        step = 6;
      } else if (input.isDone()) {
        return error("Expected a digit");
      }
    }
    if (step == 6) {
      if (input.isContinuation()) {
        c = input.head();
        if (c >= '0' && c <= '9') {
          input = input.step();
          builder.appendCodePoint(c);
          step = 7;
        } else {
          return error("Expected a digit");
        }
      } else if (input.isDone()) {
        return error("Expected a digit");
      }
    }
    if (step == 7) {
      while (input.isContinuation()) {
        c = input.head();
        if (c >= '0' && c <= '9') {
          input = input.step();
          builder.appendCodePoint(c);
        } else {
          break;
        }
      }
      if (!input.isEmpty()) {
        return done(NumberParser.valueOf(builder.toString()));
      }
    }
    if (input.isError()) {
      return error("Expected a decimal");
    }
    return new DecimalParser(builder,  step);
  }

  static Parser<Number> parse(Input input, int sign, long value) {
    final StringBuilder builder = new StringBuilder();
    if (sign < 0 && value == 0L) {
      builder.append('-').append('0');
    } else {
      builder.append(value);
    }
    return parse(input, builder,  1);
  }

  @Override
  public Parser<Number> feed(Input input) {
    return parse(input, this.builder, this.step);
  }

}
