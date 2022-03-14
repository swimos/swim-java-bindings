package ai.swim.codec.source;

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

  char head();

  Source next();

  Location location();

  boolean isDone();

  char[] collect();

  int offset();

  int len();

  char[] take(int n);

  char[] borrow(int n);

  boolean compare(char[] with);

  Source advance(int m);

  Source subInput(int start, int end);

}
