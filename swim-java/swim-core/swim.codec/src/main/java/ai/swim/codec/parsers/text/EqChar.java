package ai.swim.codec.parsers.text;

import ai.swim.codec.Parser;
import ai.swim.codec.input.Input;

public class EqChar extends Parser<Character> {
  private final char c;

  private EqChar(char c) {
    this.c = c;
  }

  public static Parser<Character> eqChar(char c) {
    return new EqChar(c);
  }

  @Override
  public Parser<Character> feed(Input input) {
    if (input.isContinuation()) {
      char head = (char) input.head();
      if (head == c) {
        input.step();
        return Parser.done(head);
      } else {
        return Parser.error(input, String.format("Expected '%s', found '%s'", c, head));
      }
    } else if (input.isDone()) {
      return Parser.error(input, "Insufficient data");
    } else {
      return this;
    }
  }
}
