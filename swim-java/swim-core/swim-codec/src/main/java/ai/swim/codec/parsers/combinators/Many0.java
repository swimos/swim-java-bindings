package ai.swim.codec.parsers.combinators;

import ai.swim.codec.Parser;
import ai.swim.codec.input.Input;
import java.util.ArrayList;
import java.util.List;

public class Many0<T> extends Parser<List<T>> {
  private final Parser<T> delegate;
  private final List<T> output;
  private Parser<T> active;

  private Many0(Parser<T> delegate) {
    this.delegate = delegate;
    this.output = new ArrayList<>();
  }

  /**
   * Repeats the delegate parser zero or more times until it produces an error and returns the output as a list.
   *
   * @param delegate to apply.
   * @param <T>      the type the parser produces.
   * @return a list of the parser's output, an error or a continuation state.
   */
  public static <T> Parser<List<T>> many0(Parser<T> delegate) {
    return new Many0<>(delegate);
  }

  @Override
  public Parser<List<T>> feed(Input input) {
    while (true) {
      if (input.isContinuation()) {
        if (active == null) {
          active = delegate.feed(input);
        }
        if (active.isDone()) {
          output.add(active.bind());
          active = null;
        } else if (active.isError()) {
          return Parser.done(output);
        } else {
          return this;
        }
      } else if (input.isDone()) {
        return Parser.done(output);
      } else if (input.isEmpty()) {
        return this;
      } else {
        throw new AssertionError();
      }
    }
  }
}
