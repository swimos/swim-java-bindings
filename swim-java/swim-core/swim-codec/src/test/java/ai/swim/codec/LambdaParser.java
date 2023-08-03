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
import java.util.function.Function;

public class LambdaParser<T> extends Parser<T> {

  private final Function<Input, Parser<T>> parseFn;

  private LambdaParser(Function<Input, Parser<T>> parseFn) {
    this.parseFn = parseFn;
  }

  public static <T> Parser<T> lambda(Function<Input, Parser<T>> fn) {
    return new LambdaParser<>(fn);
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
  public Parser<T> feed(Input input) {
    return this.parseFn.apply(input);
  }

}
