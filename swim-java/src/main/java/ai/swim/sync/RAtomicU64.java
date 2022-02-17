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
