package ai.swim.codec.parsers.text;

import ai.swim.codec.Parser;
import ai.swim.codec.input.Input;
import java.util.Arrays;

public class OneOf extends Parser<Character> {
  private final char[] chars;

  private OneOf(char[] chars) {
    this.chars = chars;
  }

  /**
   * Recognizes one of the provided characters.
   */
  public static Parser<Character> oneOf(char... chars) {
    return new OneOf(chars);
  }

  @Override
  public Parser<Character> feed(Input input) {
    if (input.isContinuation()) {
      char head = (char) input.head();

      for (char c : chars) {
        if (head == c) {
          input.step();
          return Parser.done(head);
        }
      }

      return Parser.error(input, "Expected one of: " + Arrays.toString(chars));
    } else if (input.isDone()) {
      return Parser.error(input, "Insufficient data");
    } else {
      return this;
    }
  }
}
