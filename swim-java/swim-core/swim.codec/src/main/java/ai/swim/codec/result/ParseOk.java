package ai.swim.codec.result;

import java.util.function.Function;
import ai.swim.codec.Cont;
import ai.swim.codec.input.Input;

public class ParseOk<O> implements Result<O> {

  private final Input input;
  private final O output;

  ParseOk(Input input, O output) {
    this.input = input;
    this.output = output;
  }

  @Override
  public Input getInput() {
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
