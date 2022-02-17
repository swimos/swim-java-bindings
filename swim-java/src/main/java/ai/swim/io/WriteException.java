package ai.swim.io;

import ai.swim.ffi.FfiIntrinsic;

public class WriteException extends RuntimeException {

  @FfiIntrinsic
  public WriteException() {
  }

  @FfiIntrinsic
  public WriteException(String message) {
    super(message);
  }

}
