package ai.swim.io;

import ai.swim.ffi.NativeResource;
import ai.swim.ffi.Pointer;

public class Inner implements NativeResource {

  private final long ptr;
  @SuppressWarnings({"FieldCanBeLocal", "unused"})
  private final Pointer.Destructor destructor;

  private Inner(long ptr) {
    this.ptr = ptr;
    this.destructor = Pointer.newDestructor(this, () -> Inner.deleteNative(ptr));
  }

  private static native void deleteNative(long ptr);

  public Guard lock() {
    return this.lockNative(this.ptr);
  }

  private native Guard lockNative(long ptr);

}
