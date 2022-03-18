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

import ai.swim.codec.input.Input;

public class N2 {

  private Stage stage;
  private int sign;

  public N2(Stage stage, int sign) {

  }

  enum Stage {
    Sign,
    Alt,
    Zero
  }

  public static N2 p(Input input, Stage stage, int sign) {
    int c;

    if (stage == Stage.Sign) {
      if (input.isContinuation()) {
        c = input.head();

        if (c == '-') {
          input = input.step();
          sign = -1;
        }
      }
    }

    // -0000000
    // 0xff
    // -0.1
    // 01000
    if (stage == Stage.Alt) {
      if (input.isContinuation()) {
        while (input.isContinuation()) {
          c = input.head();

          if (c == '0') {
            stage = Stage.Zero;
            continue;
          }
        }
      } else {
        return new N2(stage, sign);
      }
    }

    return new N2(stage, sign);
  }

}
