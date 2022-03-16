package ai.swim.codec.result;

import java.util.function.Function;
import java.util.function.Supplier;
import ai.swim.codec.Parser;
import ai.swim.codec.source.Source;

public class ParseIncomplete<O> implements Result<O> {

  private final Source source;
  private final int needed;
  private final Supplier<Parser<O>> parser;

  ParseIncomplete(Source source, int needed, Supplier<Parser<O>> parser) {
    this.source = source;
    this.needed = needed;
    this.parser = parser;
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

  public Supplier<Parser<O>> getParser() {
    return parser;
  }
}
