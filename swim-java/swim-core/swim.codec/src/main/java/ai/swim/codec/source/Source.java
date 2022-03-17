package ai.swim.codec.source;

import java.util.Arrays;
import ai.swim.codec.Location;

public interface Source {

  static Source string(String input) {
    return new StringSource(input);
  }

  static Source done(Source source) {
    return new SourceDone(source);
  }

  boolean complete();

  boolean has(int n);

  int head();

  Source next();

  Location location();

  boolean isDone();

  boolean isError();

  int[] collect();

  int offset();

  int len();

  int[] take(int n);

  int[] borrow(int n);

  boolean compare(int[] with);

  Source advance(int m);

  Source slice(int start, int end);

  /***
   * Compares two sources for data equality. Checking if their remaining data is equal rather than the instances.
   */
  default boolean dataEquals(Source source) {
    int thisLen = this.len();
    int thatLen = source.len();

    if (thisLen != thatLen) {
      return false;
    }

    return Arrays.equals(this.borrow(thisLen), source.borrow(thatLen));
  }

}
