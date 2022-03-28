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
import ai.swim.codec.ParserError;
import ai.swim.codec.input.Input;

import java.util.List;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.function.Function;

public class MapReduce<I, O> extends Parser<Optional<O>> {

  private final BinaryOperator<O> reduce;
  private final Function<I, O> map;
  private Parser<List<I>> parser;

  public MapReduce(Parser<List<I>> parser, Function<I, O> map, BinaryOperator<O> reduce) {
    this.parser = parser;
    this.map = map;
    this.reduce = reduce;
  }

  public static <O, T> Parser<Optional<T>> mapReduce(Parser<List<O>> parser, Function<O, T> map, BinaryOperator<T> reduce) {
    return new MapReduce<>(parser, map, reduce);
  }

  public static Parser<String> mapReduce(Parser<List<Character>> parser) {
    return mapReduce(parser, Object::toString, (acc, s) -> acc + s).map(r -> r.orElse(""));
  }

  @Override
  public Parser<Optional<O>> feed(Input input) {
    Parser<List<I>> result = this.parser.feed(input);
    if (result.isCont()) {
      this.parser = result;
      return this;
    } else if (result.isError()) {
      return Parser.error((ParserError<List<I>>) result);
    } else if (result.isDone()) {
      List<I> list = result.bind();
      return Parser.done(list.stream().map(this.map).reduce(this.reduce));
    } else {
      throw new AssertionError();
    }
  }
}
