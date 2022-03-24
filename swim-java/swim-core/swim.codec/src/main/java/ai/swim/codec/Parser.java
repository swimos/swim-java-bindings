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

import ai.swim.codec.combinators.AndThen;
import ai.swim.codec.combinators.MappedParser;
import ai.swim.codec.combinators.TryMappedParser;
import ai.swim.codec.input.Input;
import ai.swim.codec.input.InputError;
import ai.swim.codec.parsers.LambdaParser;
import ai.swim.codec.parsers.Preceded;
import ai.swim.codec.parsers.stateful.Result;
import ai.swim.codec.parsers.stateful.StatefulParser;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class Parser<O> {

  public static <O> Parser<O> lambda(Function<Input, Parser<O>> fn) {
    return new LambdaParser<>(fn);
  }

  public static <O> Parser<O> done(O output) {
    return new ParserDone<>(output);
  }

  public static <S, T> Parser<T> stateful(S state, BiFunction<S, Input, Result<S, T>> parser) {
    return new StatefulParser<>(state, parser);
  }

  public static <O> Parser<O> error(String cause) {
    return new ParserError<>(cause);
  }

  public static <O> Parser<O> error(InputError inputError) {
    return new ParserError<>(inputError.getCause());
  }

  public static <I, O> Parser<O> error(ParserError<I> error) {
    return new ParserError<>(error.getCause());
  }

  public static <B, T> Parser<T> preceded(Parser<B> by, Parser<T> then) {
    return Preceded.preceded(by, then);
  }

  public boolean isDone() {
    return false;
  }

  public boolean isError() {
    return false;
  }

  public boolean isCont() {
    return true;
  }

  public abstract Parser<O> feed(Input input);

  public O bind() {
    throw new IllegalStateException();
  }

  public <I> Parser<I> map(Function<O, I> with) {
    return MappedParser.map(this, with);
  }

  public <I> Parser<I> tryMap(Function<O, I> with) {
    return TryMappedParser.tryMap(this, with);
  }

  public <T> Parser<T> andThen(Function<O, Parser<T>> then) {
    return AndThen.andThen(this, then);
  }

}
