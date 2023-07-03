package ai.swim.codec.parsers.combinators;

import ai.swim.codec.Parser;
import ai.swim.codec.input.Input;

public abstract class TakeWhile0<S, T> extends Parser<T> {

  private final S state;

  protected TakeWhile0(S state) {
    this.state = state;
  }

  @Override
  public Parser<T> feed(Input input) {
    while (true) {
      if (input.isDone()) {
        return Parser.done(onDone(state));
      } else if (input.isContinuation()) {
        int c = input.head();

        if (onAdvance(c, state)) {
          input = input.step();
        } else {
          return Parser.done(onDone(state));
        }
      } else {
        return this;
      }
    }
  }

  protected abstract boolean onAdvance(int c, S state);

  protected abstract T onDone(S state);
}
