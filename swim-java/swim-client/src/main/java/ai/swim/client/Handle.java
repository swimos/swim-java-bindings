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

import ai.swim.lang.ffi.AtomicDestructor;
import ai.swim.lang.ffi.NativeHandle;

/**
 * A Rust SwimClient handle pointer.
 * <p>
 * Created through swim_client/src/lib.rs#Java_ai_swim_client_SwimClient_handle
 */
public class Handle implements NativeHandle, AutoCloseable {
  private final long handlePtr;
  private final AtomicDestructor destructor;

  public Handle(long handlePtr) {
    this.handlePtr = handlePtr;
    this.destructor = new AtomicDestructor(this, () -> dropHandle(handlePtr));
  }

  public static Handle create(long runtimePtr) {
    return new Handle(createHandle(runtimePtr));
  }

  private static native long createHandle(long runtimePtr);

  private static native long dropHandle(long handlePtr);

  @Override
  public void drop() {
    if (!destructor.drop()) {
      throw new IllegalStateException("Attempted to drop an already dropped SwimClient handle");
    }
  }

  @Override
  public long get() {
    return handlePtr;
  }

  @Override
  public void close() {
    destructor.drop();
  }
}