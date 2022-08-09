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

import java.math.BigInteger;

final class BigIntegerParser extends Parser<TypedNumber> {

  final int sign;
  final BigInteger value;

  private BigIntegerParser(int sign, BigInteger value) {
    this.sign = sign;
    this.value = value;
  }

  static Parser<TypedNumber> parse(Input input, int sign, BigInteger value) {
    while (input.isContinuation()) {
      final int c = input.head();
      if (c >= '0' && c <= '9') {
        input = input.step();
        value = BigInteger.TEN.multiply(value).add(BigInteger.valueOf((long) sign * (c - '0')));
      } else {
        break;
      }
    }
    if (!input.isEmpty()) {
      return done(TypedNumber.bigIntNumber(value));
    }
    return new BigIntegerParser(sign, value);
  }

  @Override
  public Parser<TypedNumber> feed(Input input) {
    return parse(input, this.sign, this.value);
  }

}
