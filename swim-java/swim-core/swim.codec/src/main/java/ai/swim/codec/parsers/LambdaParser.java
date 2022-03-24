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

package ai.swim.codec.parsers;

import ai.swim.codec.Parser;
import ai.swim.codec.input.Input;
import java.util.function.Function;

public class LambdaParser<O> extends Parser<O> {

  private final Function<Input, Parser<O>> parseFn;

  public LambdaParser(Function<Input, Parser<O>> parseFn) {
    this.parseFn = parseFn;
  }

  @Override
  public boolean isDone() {
    return false;
  }

  @Override
  public boolean isError() {
    return false;
  }

  @Override
  public boolean isCont() {
    return true;
  }

  @Override
  public Parser<O> feed(Input input) {
    return this.parseFn.apply(input);
  }

}
