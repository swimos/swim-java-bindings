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

import static ai.swim.codec.parsers.text.StringParser.decodeDigit;
import static ai.swim.codec.parsers.text.StringParser.isDigit;

final class HexadecimalParser extends Parser<TypedNumber> {

  final long value;
  final int size;

  private HexadecimalParser(long value, int size) {
    this.value = value;
    this.size = size;
  }

  private static Parser<TypedNumber> parse(Input input, long value, int size) {
    int c;
    while (input.isContinuation()) {
      c = input.head();
      if (isDigit(c)) {
        input = input.step();
        value = (value << 4) | decodeDigit(c);
        size += 1;
      } else {
        break;
      }
    }
    if (!input.isEmpty()) {
      if (size > 0) {
        if (size <= 8) {
          return done(TypedNumber.intNumber((int) value));
        } else {
          return done(TypedNumber.longNumber(value));
        }
      } else {
        return error(input, "Expected a hex digit");
      }
    }

    return new HexadecimalParser(value, size);
  }

  static Parser<TypedNumber> parse(Input input) {
    return parse(input, 0L, 0);
  }

  @Override
  public Parser<TypedNumber> feed(Input input) {
    return parse(input, this.value, this.size);
  }

}
