package ai.swim.codec.result;

import java.util.function.Function;
import ai.swim.codec.source.Source;

public class ParseOk<O> implements Result<O> {

  private final Source input;
  private final O output;

  ParseOk(Source input, O output) {
    this.input = input;
    this.output = output;
  }

  @Override
  public Source getInput() {
    return input;
  }

  @Override
  public O getOutput() {
    return this.output;
  }

  @Override
  public <F> F match(Function<ParseOk<O>, F> ok, Function<ParseError<O>, F> error, Function<ParseIncomplete<O>, F> incomplete) {
    return ok.apply(this);
  }

  @Override
  public boolean isOk() {
    return true;
  }

  @Override
  public boolean isError() {
    return false;
  }

  @Override
  public boolean isIncomplete() {
    return false;
  }

  @Override
  public <O2> Result<O2> mapOk(Function<ParseOk<O>, Result<O2>> ok) {
    return ok.apply(this);
  }

}
