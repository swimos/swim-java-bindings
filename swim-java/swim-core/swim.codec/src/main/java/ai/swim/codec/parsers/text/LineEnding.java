package ai.swim.codec.parsers.text;

import ai.swim.codec.Parser;
import ai.swim.codec.input.Input;

import static ai.swim.codec.parsers.combinators.Peek.peek;
import static ai.swim.codec.parsers.text.OneOf.oneOf;

public class LineEnding extends Parser<String> {

  private LineEnding() {

  }

  /**
   * Recognizes a line ending character.
   */
  public static Parser<String> lineEnding() {
    return preceded(peek(oneOf('\r', '\n')), new LineEnding());
  }

  @Override
  public Parser<String> feed(Input input) {
    if (input.isContinuation()) {
      char first = (char) input.head();
      if (first == '\n') {
        input.step();
        return Parser.done("\n");
      } else if (first == '\r') {
        input = input.step();
        if (input.isContinuation()) {
          char second = (char) input.head();
          if (second == '\n') {
            input.step();
            return Parser.done("\r\n");
          } else {
            return Parser.error(input, "Expected a line ending");
          }
        } else {
          return this;
        }
      } else {
        return Parser.error(input, "Expected a line ending");
      }
    } else if (input.isEmpty()) {
      return Parser.error(input, "Need more data");
    } else if (input.isDone()) {
      return Parser.error(input, "Invalid input");
    } else {
      return this;
    }
  }
}
