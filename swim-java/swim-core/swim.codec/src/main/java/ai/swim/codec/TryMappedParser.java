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

public class TryMappedParser<I, O> extends Parser<O> {

  private Parser<I> inner;
  private final Function<I, O> map;

  private TryMappedParser(Parser<I> inner, Function<I, O> map) {
    this.inner = inner;
    this.map = map;
  }

  @Override
  public Parser<O> feed(Input input) {
    try {
      this.inner = this.inner.feed(input);

      if (this.inner.isDone()) {
        return Parser.done(this.map.apply(this.inner.bind()));
      } else {
        return this;
      }
    } catch (Exception e) {
      return Parser.error(e.getMessage());
    }
  }

  @Override
  public boolean isDone() {
    return this.inner.isDone();
  }

  @Override
  public boolean isCont() {
    return this.inner.isCont();
  }

  @Override
  public boolean isError() {
    return this.inner.isError();
  }

  public static <I, O> TryMappedParser<I, O> tryMap(Parser<I> parser, Function<I, O> with) {
    return new TryMappedParser<>(parser, with);
  }
}
