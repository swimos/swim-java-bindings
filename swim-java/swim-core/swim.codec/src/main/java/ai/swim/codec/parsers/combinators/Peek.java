package ai.swim.codec.parsers.combinators;

import ai.swim.codec.Parser;
import ai.swim.codec.input.Input;

public class Peek<T> extends Parser<T> {
  private Parser<T> parser;

  private Peek(Parser<T> parser) {
    this.parser = parser;
  }

  /**
   * Applies a parser without advancing its input.
   *
   * @param delegate to apply
   * @param <T>      the type the parser produces.
   * @return the parser's output, an error or a continuation.
   */
  public static <T> Parser<T> peek(Parser<T> delegate) {
    return new Peek<>(delegate);
  }

  @Override
  public Parser<T> feed(Input input) {
    parser = parser.feed(input.clone());
    if (parser.isCont()) {
      return this;
    } else {
      return parser;
    }
  }
}
