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

package ai.swim.codec;

import ai.swim.codec.input.Input;

public class ParserDone<O> extends Parser<O> {
  private final O output;

  public ParserDone(O output) {
    this.output = output;
  }

  @Override
  public Parser<O> feed(Input input) {
    throw new IllegalStateException();
  }

  @Override
  public O bind() {
    return this.output;
  }

  @Override
  public boolean isDone() {
    return true;
  }

  @Override
  public boolean isCont() {
    return false;
  }
}
