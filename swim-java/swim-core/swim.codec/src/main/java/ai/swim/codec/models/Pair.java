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

package ai.swim.codec.models;

public class Pair<O1, O2> {

  private final O1 output1;
  private final O2 output2;

  public Pair(O1 output1, O2 output2) {
    this.output1 = output1;
    this.output2 = output2;
  }

  public O1 getOutput1() {
    return output1;
  }

  public O2 getOutput2() {
    return output2;
  }

}
