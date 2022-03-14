package ai.swim.codec.result;

import java.util.function.Function;
import ai.swim.codec.Location;
import ai.swim.codec.source.Source;

public class ParseError<O> implements Result<O> {

  private final Source source;
  private final Location location;
  private final String cause;

  ParseError(Source source, String cause) {
    this.source = source;
    this.location = source.location();
    this.cause = cause;
  }

  @Override
  public boolean isOk() {
    return false;
  }

  @Override
  public boolean isError() {
    return true;
  }

  @Override
  public boolean isIncomplete() {
    return false;
  }

  @Override
  public Source getInput() {
    return this.source;
  }

  @Override
  public <F> F match(Function<ParseOk<O>, F> ok, Function<ParseError<O>, F> error, Function<ParseIncomplete<O>, F> incomplete) {
    return error.apply(this);
  }

  public Location getLocation() {
    return location;
  }

  public String getCause() {
    return cause;
  }

}
