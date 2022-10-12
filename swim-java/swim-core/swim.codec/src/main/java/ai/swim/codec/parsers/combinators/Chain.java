package ai.swim.codec.parsers.combinators;

import ai.swim.codec.Parser;
import ai.swim.codec.ParserError;
import ai.swim.codec.input.Input;

public class Chain<T> extends Parser<T> {
  private final Parser<?>[] parsers;
  private int idx;

  private Chain(Parser<?>[] parsers) {
    this.parsers = parsers;
    this.idx = 0;
  }

  /**
   * Runs n parsers and binds the final parser if it succeeds.
   */
  public static <T> Parser<T> chain(Parser<?>... parsers) {
    return new Chain<>(parsers);
  }

  @Override
  public Parser<T> feed(Input input) {
    while (true) {
      if (input.isContinuation()) {
        Parser<?> parser = parsers[idx];

        if (parser.isError()) {
          return Parser.error(input, ((ParserError<?>) parser).cause());
        }

        parser = parser.feed(input);

        if (parser.isError()) {
          return Parser.error(input, ((ParserError<?>) parser).cause());
        } else if (parser.isDone()) {
          if (idx == parsers.length - 1) {
            //noinspection unchecked
            return Parser.done((T) parser.bind());
          } else {
            parsers[idx] = parser;
            idx += 1;
          }
        } else if (parser.isCont()) {
          parsers[idx] = parser;
          return this;
        }
      } else if (input.isDone()) {
        Parser<?> parser = parsers[idx];
        if (parser.isCont()) {
          return Parser.error(input, "Expected more data");
        } else {
          throw new AssertionError("Unexpected state");
        }
      } else {
        return this;
      }
    }
  }
}
