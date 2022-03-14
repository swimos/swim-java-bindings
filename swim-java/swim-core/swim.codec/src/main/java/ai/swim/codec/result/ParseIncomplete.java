package ai.swim.codec.result;

import java.util.function.Function;
import ai.swim.codec.source.Source;

public class ParseIncomplete<O> implements Result<O> {

  private final Source source;
  private final int needed;

  ParseIncomplete(Source source, int needed) {
    this.source = source;
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
  public Source getInput() {
    return this.source;
  }

  @Override
  public <F> F match(Function<ParseOk<O>, F> ok, Function<ParseError<O>, F> error, Function<ParseIncomplete<O>, F> incomplete) {
    return incomplete.apply(this);
  }

  public int getNeeded() {
    return needed;
  }

}
