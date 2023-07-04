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

package ai.swim.lang.ffi;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A wrapper around a native resource deallocation function to which it provides exclusive access and guards against
 * double-freeing.
 */
public class AtomicDestructor {
  private static final int LIVE = 0;
  private static final int DROPPING = 1 << 1;
  private static final int DROPPED = 1 << 2;

  private final AtomicInteger state;
  @SuppressWarnings({"FieldCanBeLocal", "unused"})
  private final Pointer.Destructor destructor;
  private final Pointer.Destruct callback;

  public AtomicDestructor(NativeResource referent, Pointer.Destruct callback) {
    this.state = new AtomicInteger(LIVE);
    this.destructor = Pointer.newDestructor(referent, this::drop);
    this.callback = callback;
  }

  /**
   * Attempts to drop the native resource.
   *
   * @return whether the resource was successfully deallocated. This will only ever return true *once*; repeated calls
   * to this method after the resource has been deallocated will return {@code false}.
   */
  public boolean drop() {
    while (true) {
      int old = state.get();
      if ((old & DROPPING) != 0 || (old & DROPPED) != 0) {
        return false;
      } else {
        int newState = old | DROPPING;
        if (state.compareAndSet(old, newState)) {
          callback.call();
          state.set(DROPPED);
          return true;
        }
      }
    }
  }

}
