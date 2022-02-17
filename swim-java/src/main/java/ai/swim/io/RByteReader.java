package ai.swim.io;

import ai.swim.ffi.NativeResource;
import ai.swim.ffi.Pointer;

// todo implement close
public class RByteReader implements NativeResource {

  private final long writePtr;
  @SuppressWarnings({"FieldCanBeLocal", "unused"})
  private final Pointer.Destructor destructor;

  private RByteReader(long writePtr) {
    this.writePtr = writePtr;
    this.destructor = Pointer.newDestructor(this, () -> RByteReader.releaseNative(this.writePtr));
  }

  public static RByteReader create(int capacity, DidReadCallback didRead, DidCloseCallback didClose) {
    long ptr = RByteReader.createNative(capacity, didRead, didClose);
    return new RByteReader(ptr);
  }

  private static native long createNative(int capacity, DidReadCallback callback, DidCloseCallback didClose);

  private static native void releaseNative(long readPtr);

  @FunctionalInterface
  public interface DidReadCallback {
    void didRead(byte[] bytes);
  }

  @FunctionalInterface
  public interface DidCloseCallback {
    void didClose();
  }

}
