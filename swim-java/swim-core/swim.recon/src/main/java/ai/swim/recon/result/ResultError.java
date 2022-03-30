package ai.swim.recon.result;

public class ResultError<O> extends ParseResult<O> {
  private final String cause;

  public ResultError(String cause) {
    this.cause = cause;
  }

  public String getCause() {
    return cause;
  }

  @Override
  public boolean isError() {
    return true;
  }

  @Override
  public <T> ParseResult<T> cast() {
    return new ResultError<>(this.cause);
  }

  @Override
  public String toString() {
    return "ResultError{" +
        "cause='" + cause + '\'' +
        '}';
  }
}
