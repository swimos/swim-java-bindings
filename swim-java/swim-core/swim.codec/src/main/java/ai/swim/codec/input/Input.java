package ai.swim.codec.input;

import ai.swim.codec.Location;

public interface Input {

  static Input string(String input) {
    return new StringInput(input);
  }

  static Input done(Input input) {
    return new InputDone(input);
  }

  boolean complete();

  boolean has(int n);

  char head();

  Input next();

  Location location();

  boolean isDone();

  char[] collect();

  int offset();

  int len();

  char[] take(int n);

  char[] borrow(int n);

  boolean compare(char[] with);

  Input advance(int m);

  Input subInput(int start, int end);

}
