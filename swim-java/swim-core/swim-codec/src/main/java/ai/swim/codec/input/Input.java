/*
 * Copyright 2015-2024 Swim Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.swim.codec.input;

import ai.swim.codec.data.ReadBuffer;
import ai.swim.codec.location.Location;
import java.nio.ByteBuffer;

/**
 * A non-blocking symbol reader that provides single and multiple symbol lookahead.
 * <p>
 * The symbols that an {@code Input} provides are modelled as primitive {@code int}s that commonly represent Unicode
 * code points or raw octets.
 */
public abstract class Input {

  /**
   * Creates a new string {@code Input}.
   */
  public static StringInput string(String input) {
    return new StringInput(input);
  }

  /**
   * Creates a new {@link ByteBuffer} {@code Input}.
   */
  public static ByteBufferInput byteBuffer(ByteBuffer data) {
    return new ByteBufferInput(data);
  }

  /**
   * Creates a new {@link ReadBuffer} {@code Input}.
   */
  public static ReadBufferInput readBuffer(ReadBuffer data) {
    return new ReadBufferInput(data);
  }

  /**
   * Creates a new {@code Input} in the done state.
   */
  public static Input done(Input input) {
    return new InputDone(input);
  }

  /**
   * Returns whether this {@code Input} has n symbols available.
   */
  public abstract boolean has(int n);

  /**
   * Returns the current lookahead symbol, if this {@code Input} is in the
   * <em>continuation</em> state.
   *
   * @throws IllegalStateException if this {@code Input} is not in the <em>cont</em>
   *                               state.
   */
  public abstract int head();

  /**
   * Returns an {@code Input} equivalent to this {@code Input}, but advanced to
   * the next symbol.
   */
  public abstract Input step();

  /**
   * Returns the location of the offset into this {@code Input}.
   * <p>
   * For a string {@code Input} this will provide a line, column and offset index into the {@code Input}.
   */
  public abstract Location location();

  /**
   * Returns if the {@code Input} is not able to produce another symbol if it is advanced and another one will not be
   * available in the future.
   */
  public abstract boolean isDone();

  /**
   * Returns if the {@code Input} is able to produce a symbol if it is advanced.
   */
  public abstract boolean isContinuation();

  /**
   * Returns if the {@code Input} is not able to produce another symbol if it is advanced but a symbol may be available
   * in the future.
   */
  public abstract boolean isEmpty();

  /**
   * Set this {@code Input} to be a partial representation of the data. If true, then this {@code Input} represents
   * a segment of a larger piece of data. If false, then this {@code Input} is the final segment of the data and no more
   * will be available.
   * <p>
   * Analogous to fragmented websocket frames.
   */
  public abstract Input setPartial(boolean isPartial);

  /**
   * Binds the remaining symbols that are available in this {@code Input}.
   */
  public int[] bind() {
    int[] into = new int[this.len()];
    bind(into);

    return into;
  }

  /**
   * Binds the remaining symbols into the provided array.
   */
  public abstract void bind(int[] into);

  /**
   * Returns the difference between the source length of this {@code Input} and its offset.
   */
  public abstract int len();

  /**
   * Takes n symbols from the source.
   *
   * @throws IllegalStateException if this {@code Input} has insufficient data available.
   */
  public int[] take(int n) {
    int[] into = new int[n];
    take(into);

    return into;
  }

  /**
   * Populates the provided array with as many symbols that are available.
   */
  public abstract void take(int[] into);

  /**
   * Returns an independently positioned view into the symbol stream,
   * initialized with identical state to this {@code Input}.
   */
  @Override
  public abstract Input clone();

  /**
   * Replaces this {@code Input}'s contents from the provided {@code Input} and strip off any already read symbols.
   */
  public void setFrom(Input innerInput) {
    throw new IllegalStateException();
  }

  /**
   * Extend this instance with the data from the argument. This operation should ignore any indices from the argument
   * and just extend the data.
   *
   * @param from to pull the data from.
   * @return an extended Input instance.
   * @throws IllegalArgumentException if the type of the argument is not the same as this instance.
   */
  public abstract Input extend(Input from);

}
