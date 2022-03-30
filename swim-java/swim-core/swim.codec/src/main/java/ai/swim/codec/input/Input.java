package ai.swim.codec.input;

import ai.swim.codec.location.Location;

public abstract class Input {

  public static Input string(String input) {
    return new StringInput(input);
  }

  public static Input done(Input input) {
    return new InputDone(input);
  }

  public abstract boolean has(int n);

  public abstract int head();

  public abstract Input step();

  public abstract Location location();

  public abstract boolean isDone();

  public abstract boolean isContinuation();

  public abstract boolean isEmpty();

  public abstract Input isPartial(boolean isPartial);

  public abstract boolean isError();

  public abstract int[] bind();

  public abstract int len();

  public abstract int[] take(int n);

  public abstract boolean compare(int[] with);

  @Override
  public abstract Input clone();

  public void cloneFrom(Input innerInput) {
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
