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
