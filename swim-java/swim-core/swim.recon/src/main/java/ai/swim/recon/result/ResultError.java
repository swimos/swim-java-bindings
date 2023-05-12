package ai.swim.recon.result;

import ai.swim.codec.location.Location;

public class ResultError<O> extends ParseResult<O> {
  private final String cause;
  private final Location location;

  public ResultError(String cause, Location location) {
    this.cause = cause;
    this.location = location;
  }

  public String getCause() {
    return cause;
  }

  public Location getLocation() {
    return location;
  }

  @Override
  public boolean isError() {
    return true;
  }

  @Override
  public <T> ParseResult<T> cast() {
    return new ResultError<>(cause, location);
  }

  @Override
  public String toString() {
    return "ResultError{" +
            "cause='" + cause + '\'' +
            ", location=" + location +
            '}';
  }
}
