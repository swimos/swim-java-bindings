package ai.swim.sync;

import ai.swim.ffi.NativeResource;
import ai.swim.ffi.Pointer;

public class RAtomicU64 implements NativeResource {

  @SuppressWarnings({"FieldCanBeLocal", "unused"})
  private final Pointer.Destructor destructor;
  private final long ptr;

  private RAtomicU64(long ptr) {
    this.destructor = Pointer.newDestructor(this, () -> RAtomicU64.destroyNative(ptr));
    this.ptr = ptr;
  }

  public static RAtomicU64 create(long initial) {
    if (initial < 0) {
      throw new IllegalArgumentException("Attempted to create an RAtomicU64 with a negative value");
    }

    long ptr = RAtomicU64.createNative(initial);
    return new RAtomicU64(ptr);
  }

  private static native long createNative(long initial);

  private static native void destroyNative(long ptr);

  public long load(Ordering ordering) {
    return this.loadNative(this.ptr, ordering.getOrdinal());
  }

  private native long loadNative(long ptr, int ordinal);

  public void store(long value, Ordering ordering) {
    if (value < 0) {
      throw new IllegalArgumentException("Attempted to store a negative value in an RAtomicU64");
    }

    this.storeNative(this.ptr, value, ordering.getOrdinal());
  }

  private native void storeNative(long ptr, long value, int ordinal);

}
