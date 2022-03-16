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

package ai.swim.codec.num;

import ai.swim.codec.Cont;
import ai.swim.codec.Parser;
import ai.swim.codec.result.Result;

public class N2 {

  enum State {
    Init,
    ParsingDigit,
    ParsingBigInt,
    ParsingHexadecimal
  }

  public static Parser<Number> number() {


    return null;
  }

  private static Parser<Number> execNumber(State state, int sign, long value, int mode, int step) {
    return input -> {
      if (input.isDone()) {
        return Cont.none(Result.incomplete(input, 1, () -> execNumber(state, sign, value, mode, step)));
      } else {
        return null;
      }
    };
  }

}
