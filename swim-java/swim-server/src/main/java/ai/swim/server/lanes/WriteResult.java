package ai.swim.server.lanes;

public enum WriteResult {
  NoData(0),
  Done(0),
  DataStillAvailable(1);

  private final byte statusCode;

  WriteResult(int statusCode) {
    this.statusCode = (byte) statusCode;
  }

  public boolean done() {
    return this == NoData || this == Done;
  }

  public byte statusCode() {
    return statusCode;
  }
}
