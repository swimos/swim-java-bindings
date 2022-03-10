package ai.swim.codec;

import java.util.function.Function;
import ai.swim.codec.input.Input;

public interface Result<I, O> {

  static <I, O> Result<I, O> ok(Input<I> input, O output) {
    return new ParseOk<>(input, output);
  }

  static <I, O> Result<I, O> error(Input<I> input, String cause) {
    return new ParseError<>(input, cause);
  }

  static <I, O> Result<I, O> incomplete(Input<I> input, int needed) {
    return new ParseIncomplete<>(input, needed);
  }

  boolean isOk();

  boolean isError();

  boolean isIncomplete();

  Input<I> getInput();

  default O getOutput() {
    throw new AssertionError();
  }

  <F> F match(Function<ParseOk<I, O>, F> ok, Function<ParseError<I, O>, F> error, Function<ParseIncomplete<I, O>, F> incomplete);

  @SuppressWarnings("unchecked")
  default <NI, NO> Result<NI, NO> cast() {
    return (Result<NI, NO>) this;
  }

}

class ParseOk<I, O> implements Result<I, O> {

  private final Input<I> input;
  private final O output;

  ParseOk(Input<I> input, O output) {
    this.input = input;
    this.output = output;
  }

  @Override
  public Input<I> getInput() {
    return input;
  }

  @Override
  public O getOutput() {
    return this.output;
  }

  @Override
  public <F> F match(Function<ParseOk<I, O>, F> ok, Function<ParseError<I, O>, F> error, Function<ParseIncomplete<I, O>, F> incomplete) {
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

//  public O merge(ParseOk<I, O> ok2) {
//    return new ParseOk<>();
//  }

}

class ParseError<I, O> implements Result<I, O> {

  private final Input<I> input;
  private final Location location;
  private final String cause;

  ParseError(Input<I> input, String cause) {
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
  public Input<I> getInput() {
    return this.input;
  }

  @Override
  public <F> F match(Function<ParseOk<I, O>, F> ok, Function<ParseError<I, O>, F> error, Function<ParseIncomplete<I, O>, F> incomplete) {
    return error.apply(this);
  }

  public Location getLocation() {
    return location;
  }

  public String getCause() {
    return cause;
  }

}

class ParseIncomplete<I, O> implements Result<I, O> {

  private final Input<I> input;
  private final int needed;

  ParseIncomplete(Input<I> input, int needed) {
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
  public Input<I> getInput() {
    return this.input;
  }

  @Override
  public <F> F match(Function<ParseOk<I, O>, F> ok, Function<ParseError<I, O>, F> error, Function<ParseIncomplete<I, O>, F> incomplete) {
    return incomplete.apply(this);
  }

  public int getNeeded() {
    return needed;
  }

}