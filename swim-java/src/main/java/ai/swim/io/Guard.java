package ai.swim.io;

import ai.swim.ffi.NativeResource;
import ai.swim.ffi.Pointer;

public class Guard implements NativeResource, AutoCloseable {

  private final long ptr;
  @SuppressWarnings({"FieldCanBeLocal", "unused"})
  private final Pointer.Destructor destructor;

  public Guard(long ptr) {
    this.ptr = ptr;
    this.destructor = Pointer.newDestructor(this, () -> {
      // This indicates a logic issue where the guard was not released and was instead released by the native resource
      // deallocator thread.
      throw new RuntimeException("Byte channel guard not freed manually. This is a bug");
    });
  }

  public void unlock() {
    this.unlock(this.ptr);
  }

  private native Guard unlock(long ptr);

  @Override
  public void close() {
    this.unlock();
  }

}
