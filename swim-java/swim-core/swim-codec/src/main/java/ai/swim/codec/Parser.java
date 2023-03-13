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
import ai.swim.codec.location.Location;
import ai.swim.codec.parsers.Preceded;
import ai.swim.codec.parsers.combinators.AndThen;
import ai.swim.codec.parsers.combinators.MappedParser;
import ai.swim.codec.parsers.combinators.TryMappedParser;

import java.util.function.Function;

/**
 * An incremental parser.
 *
 * @param <O> the type this parser produces.
 */
public abstract class Parser<O> {

  /**
   * Creates a parser in the done state that will bind the provided output.
   */
  public static <O> Parser<O> done(O output) {
    return new ParserDone<>(output);
  }

  /**
   * Creates a parser in an error state. The error is spanned by the location in the input and will has a description of
   * the cause provided.
   */
  public static <O> Parser<O> error(Input input, String cause) {
    return new ParserError<>(input.location(), cause);
  }

  /**
   * Creates a new parser in the error state from the provided {@code ParserError} and spanned by {@code Location}.
   */
  public static <I, O> Parser<O> error(Location location, ParserError<I> error) {
    return new ParserError<>(location, error.cause());
  }

  /**
   * Runs parser {@code by} and if it succeeds then the output is discarded and {@code then} is returned.
   */
  public static <B, T> Parser<T> preceded(Parser<B> by, Parser<T> then) {
    return Preceded.preceded(by, then);
  }

  /**
   * Returns whether this parser is in the done state.
   */
  public boolean isDone() {
    return false;
  }

  /**
   * Returns whether this parser is in an error state.
   */
  public boolean isError() {
    return false;
  }

  /**
   * Returns whether this parser is in a continuation state.
   */
  public boolean isCont() {
    return true;
  }

  /**
   * Incrementally parses as much data as possible from {@code input} and returns a new parser that represents how to
   * parse more data.
   */
  public abstract Parser<O> feed(Input input);

  /**
   * Returns the parsed result. Only guaranteed to return a result when in the
   * <em>done</em> state.
   *
   * @throws IllegalStateException if this {@code Parser} is not in the
   *                               <em>done</em> state.
   */
  public O bind() {
    throw new IllegalStateException();
  }

  /**
   * Maps the output of this parser if it successfully produced an output.
   */
  public <I> Parser<I> map(Function<O, I> with) {
    return MappedParser.map(this, with);
  }

  /**
   * Attempts to map the output of this parser if it successfully produced an output. If an exception is thrown, then
   * a {@code ParserError} is returned with a cause that is a string representation of the exception's cause.
   */
  public <I> Parser<I> tryMap(Function<O, I> with) {
    return TryMappedParser.tryMap(this, with);
  }

  /**
   * Runs this parser and if it succeeds then the function is invoked with its output and a new parser is returned.
   */
  public <T> Parser<T> andThen(Function<O, Parser<T>> then) {
    return AndThen.andThen(this, then);
  }

}
