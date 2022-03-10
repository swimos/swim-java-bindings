package ai.swim.codec.input;

import ai.swim.codec.Location;

public class InputDone<I> implements Input<I> {

  private final Location location;

  public InputDone(Location location) {
    this.location = location;
  }

  @Override
  public boolean complete() {
    return false;
  }

  @Override
  public I head() {
    throw new IllegalStateException();
  }

  @Override
  public Input<I> next() {
    throw new IllegalStateException();
  }

  @Override
  public Location location() {
    return this.location;
  }

  @Override
  public boolean isDone() {
    return true;
  }

}
