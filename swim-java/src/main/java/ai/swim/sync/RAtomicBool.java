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
