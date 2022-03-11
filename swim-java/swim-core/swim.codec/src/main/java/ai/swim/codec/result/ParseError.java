package ai.swim.codec.result;

import java.util.function.Function;
import ai.swim.codec.Location;
import ai.swim.codec.input.Input;

public class ParseError<O> implements Result<O> {

  private final Input input;
  private final Location location;
  private final String cause;

  ParseError(Input input, String cause) {
    this.input = input;
    this.location = input.location();
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
  public Input getInput() {
    return this.input;
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
