// Copyright 2015-2021 Swim Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package ai.swim.ffi.io;

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
