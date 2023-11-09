package ai.swim.server;

import ai.swim.lang.ffi.AtomicDestructor;
import ai.swim.lang.ffi.NativeResource;

/**
 * A handle to a running Swim Server instance.
 */
public class SwimServerHandle implements NativeResource {
  private final AtomicDestructor destructor;

  SwimServerHandle(long handlePtr) {
    this.destructor = new AtomicDestructor(this, () -> dropHandle(handlePtr));
  }

  private static native long dropHandle(long handlePtr);

  /**
   * Stops the server.
   *
   * @throws IllegalStateException if the server has already been stopped.
   */
  public void stop() {
    if (!destructor.drop()) {
      throw new IllegalStateException("Attempted to drop an already dropped SwimServerHandle");
    }
  }

}
