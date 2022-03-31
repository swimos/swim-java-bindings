package ai.swim.codec.input;

import ai.swim.codec.location.Location;

/**
 * A non-blocking token stream reader that provides single and multiple token lookahead.
 * <p>
 * The tokens that an {@code Input} provides are modelled as primitive {@code int}s that commonly represent Unicode
 * code points or raw octets.
 */
public abstract class Input {

  /**
   * Creates a new string {@code Input}.
   */
  public static Input string(String input) {
    return new StringInput(input);
  }

  /**
   * Creates a new {@code Input} in the done state.
   */
  public static Input done(Input input) {
    return new InputDone(input);
  }

  /**
   * Returns whether this {@code Input} has n tokens available.
   */
  public abstract boolean has(int n);

  /**
   * Returns the current lookahead token, if this {@code Input} is in the
   * <em>continuation</em> state.
   *
   * @throws IllegalStateException if this {@code Input} is not in the <em>cont</em>
   *                               state.
   */
  public abstract int head();

  /**
   * Returns an {@code Input} equivalent to this {@code Input}, but advanced to
   * the next token. Returns an {@code Input} in the <em>error</em> state if
   * this {@code Input} is not in the <em>cont</em> state.
   */
  public abstract Input step();

  /**
   * Returns the location of the offset into this {@code Input}.
   * <p>
   * For a string {@code Input} this will provide a line, column and offset index into the {@code Input}.
   */
  public abstract Location location();

  /**
   * Returns if the {@code Input} is not able to produce another token if it is advanced and another one will not be
   * available in the future.
   */
  public abstract boolean isDone();

  /**
   * Returns if the {@code Input} is able to produce a token if it is advanced.
   */
  public abstract boolean isContinuation();

  /**
   * Returns if the {@code Input} is not able to produce another token if it is advanced but a token may be available
   * in the future.
   */
  public abstract boolean isEmpty();

  /**
   * Set this {@code Input} to be a partial representation of the data.
   */
  public abstract Input isPartial(boolean isPartial);

  /**
   * Returns whether this {@code Input} is in the error state.
   */
  public abstract boolean isError();

  /**
   * Binds the remaining tokens that are available in this {@code Input} and advances the offset.
   */
  public abstract int[] bind();

  /**
   * Returns the difference between the source length of this {@code Input} and its offset.
   */
  public abstract int len();

  /**
   * Takes n tokens from the source.
   *
   * @throws IllegalStateException if this {@code Input} has insufficient data available.
   */
  public abstract int[] take(int n);

  /**
   * Returns an independently positioned view into the token stream,
   * initialized with identical state to this {@code Input}.
   */
  @Override
  public abstract Input clone();

  /**
   * Replaces this {@code Input}'s contents from the provided {@code Input} and strip off any already read tokens.
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
