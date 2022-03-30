package ai.swim.codec.input;

import ai.swim.codec.location.Location;

import java.util.Arrays;

public abstract class Input {

  public static Input string(String input) {
    return new StringInput(input);
  }

  public static Input done(Input input) {
    return new InputDone(input);
  }

  public abstract boolean complete();

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

  public abstract int offset();

  public abstract int len();

  public abstract int[] take(int n);

  public abstract int[] borrow(int n);

  public abstract boolean compare(int[] with);

  public abstract Input advance(int m);

  public abstract Input slice(int start, int end);

  @Override
  public abstract Input clone();


  /***
   * Compares two sources for data equality. Checking if their remaining data is equal rather than the instances.
   */
  public boolean dataEquals(Input input) {
    int thisLen = this.len();
    int thatLen = input.len();

    if (thisLen != thatLen) {
      return false;
    }

    return Arrays.equals(this.borrow(thisLen), input.borrow(thatLen));
  }

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
