package ai.swim.recon.result;

public class ResultError<O> extends ParseResult<O> {
  private final String cause;

  public ResultError(String cause) {
    this.cause = cause;
  }

  public String getCause() {
    return cause;
  }
}
