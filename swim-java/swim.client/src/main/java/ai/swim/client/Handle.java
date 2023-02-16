// Copyright 2015-2022 Swim Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package ai.swim.client;

import ai.swim.lang.ffi.NativeResource;
import ai.swim.lang.ffi.Pointer;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A Rust SwimClient handle pointer.
 * <p>
 * Created through swim_client/src/lib.rs#Java_ai_swim_client_SwimClient_handle
 */
public class Handle implements NativeResource {
  private final long handlePtr;
  @SuppressWarnings({"FieldCanBeLocal", "unused"})
  private final Pointer.Destructor destructor;
  private final AtomicBoolean dropped;

  public Handle(long handlePtr) {
    AtomicBoolean dropped = new AtomicBoolean(false);
    this.handlePtr = handlePtr;
    this.dropped = dropped;
    this.destructor = Pointer.newDestructor(this, () -> {
      if (!dropped.get()) {
        Handle.dropHandle(this.handlePtr);
      }
    });
  }

  public static Handle create(long runtimePtr) {
    return new Handle(createHandle(runtimePtr));
  }

  private static native long createHandle(long runtimePtr);

  private static native long dropHandle(long handlePtr);

  public void drop() {
    if (dropped.get()) {
      throw new IllegalStateException("Attempted to drop an already dropped SwimClient handle");
    } else {
      dropHandle(handlePtr);
      dropped.set(true);
    }
  }

  public long get() {
    return handlePtr;
  }

}
