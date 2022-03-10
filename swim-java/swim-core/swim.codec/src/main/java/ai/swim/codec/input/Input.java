package ai.swim.codec.input;

import ai.swim.codec.Location;

public interface Input<I> {

  static Input<Character> string(String input) {
    return new StringInput(input);
  }

  static <F> Input<F> done(Location location) {
    return new InputDone<>(location);
  }

  boolean complete();

  I head();

  Input<I> next();

  Location location();

  boolean isDone();

}
