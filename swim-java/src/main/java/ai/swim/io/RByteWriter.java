package ai.swim.io;

import ai.swim.ffi.NativeResource;
import ai.swim.ffi.Pointer;

// todo implement close
public class RByteWriter implements NativeResource {

  private final long writePtr;
  private final Object lock;

  @SuppressWarnings({"FieldCanBeLocal", "unused"})
  private final Pointer.Destructor destructor;

  private RByteWriter(long writePtr) {
    this.writePtr = writePtr;
    this.destructor = Pointer.newDestructor(this, () -> RByteWriter.releaseNative(this.writePtr));
    this.lock = new Object();
  }

  public static RByteWriter create(int capacity) {
    long ptr = RByteWriter.createNative(capacity);
    return new RByteWriter(ptr);
  }

  private static native long createNative(int capacity);

  private static native void releaseNative(long readPtr);

  private static native int tryWrite(long ptr, byte[] bytes) throws WriteException;

  private static native void write(long ptr, byte[] bytes, Object lock) throws WriteException;

  public int tryWrite(byte[] bytes) throws WriteException {
    return RByteWriter.tryWrite(this.writePtr, bytes);
  }

  public void write(byte[] bytes) throws WriteException {
    synchronized (this.lock) {
      RByteWriter.write(this.writePtr, bytes, this.lock);
    }
  }
}
