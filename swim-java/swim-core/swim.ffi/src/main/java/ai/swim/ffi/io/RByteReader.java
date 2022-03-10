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
