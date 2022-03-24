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
import static ai.swim.codec.parsers.string.StringParser.decodeDigit;
import static ai.swim.codec.parsers.string.StringParser.isDigit;

final class HexadecimalParser extends Parser<Number> {

  final long value;
  final int size;

  HexadecimalParser(long value, int size) {
    this.value = value;
    this.size = size;
  }

  static Parser<Number> parse(Input input, long value, int size) {
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
          return done((int) value);
        } else {
          return done(value);
        }
      } else {
        return error("Expected a hex digit");
      }
    }
    if (input.isError()) {
      return error("Expected a hex digit");
    }
    return new HexadecimalParser(value, size);
  }

  static Parser<Number> parse(Input input) {
    return parse(input, 0L, 0);
  }

  @Override
  public Parser<Number> feed(Input input) {
    return parse(input, this.value, this.size);
  }

}
