package ai.swim.sync;

import ai.swim.ffi.NativeResource;
import ai.swim.ffi.Pointer;

public final class RAtomicBool implements NativeResource {

  @SuppressWarnings({"FieldCanBeLocal", "unused"})
  private final Pointer.Destructor destructor;
  private final long ptr;

  private RAtomicBool(long ptr) {
    this.destructor = Pointer.newDestructor(this, () -> RAtomicBool.destroyNative(ptr));
    this.ptr = ptr;
  }

  public static RAtomicBool create(boolean initial) {
    long ptr = RAtomicBool.createNative(initial);
    return new RAtomicBool(ptr);
  }

  private static native long createNative(boolean initial);

  private static native void destroyNative(long ptr);

  public boolean load(Ordering ordering) {
    return this.loadNative(this.ptr, ordering.getOrdinal());
  }

  private native boolean loadNative(long ptr, int ordinal);

  public void store(boolean value, Ordering ordering) {
    this.storeNative(this.ptr, value, ordering.getOrdinal());
  }

  private native void storeNative(long ptr, boolean value, int ordinal);

}
