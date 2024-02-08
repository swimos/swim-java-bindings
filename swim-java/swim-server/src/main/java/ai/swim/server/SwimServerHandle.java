/*
 * Copyright 2015-2024 Swim Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.swim.server;

import ai.swim.lang.ffi.AtomicDestructor;
import ai.swim.lang.ffi.NativeResource;

/**
 * A handle to a running Swim Server instance.
 */
public class SwimServerHandle implements NativeResource {
  private final AtomicDestructor destructor;

  SwimServerHandle(long handlePtr) {
    this.destructor = new AtomicDestructor(this, () -> dropHandle(handlePtr));
  }

  private static native long dropHandle(long handlePtr);

  /**
   * Stops the server.
   *
   * @throws IllegalStateException if the server has already been stopped.
   */
  public void stop() {
    if (!destructor.drop()) {
      throw new IllegalStateException("Attempted to drop an already dropped SwimServerHandle");
    }
  }

}
