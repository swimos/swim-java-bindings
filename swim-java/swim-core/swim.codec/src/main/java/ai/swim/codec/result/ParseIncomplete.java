package ai.swim.codec.result;

import java.util.function.Function;
import ai.swim.codec.input.Input;

public class ParseIncomplete<O> implements Result<O> {

  private final Input input;
  private final int needed;

  ParseIncomplete(Input input, int needed) {
    this.input = input;
    this.needed = needed;
  }

  @Override
  public boolean isOk() {
    return false;
  }

  @Override
  public boolean isError() {
    return false;
  }

  @Override
  public boolean isIncomplete() {
    return true;
  }

  @Override
  public Input getInput() {
    return this.input;
  }

  @Override
  public <F> F match(Function<ParseOk<O>, F> ok, Function<ParseError<O>, F> error, Function<ParseIncomplete<O>, F> incomplete) {
    return incomplete.apply(this);
  }

  public int getNeeded() {
    return needed;
  }

}
