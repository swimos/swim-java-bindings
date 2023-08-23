package ai.swim.server;

import ai.swim.lang.ffi.AtomicDestructor;
import ai.swim.lang.ffi.NativeResource;

public class SwimServerHandle implements NativeResource {
  private final AtomicDestructor destructor;

  SwimServerHandle(long handlePtr) {
    this.destructor = new AtomicDestructor(this, () -> dropHandle(handlePtr));
  }

  private static native long dropHandle(long handlePtr);

  public void drop() {
    if (!destructor.drop()) {
      throw new IllegalStateException("Attempted to drop an already dropped SwimServerHandle");
    }
  }

}
