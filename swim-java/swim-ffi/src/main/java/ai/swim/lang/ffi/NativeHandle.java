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

package ai.swim.lang.ffi;

/**
 * A native resource handle that can be provided to classes that are not concerned about their implementation details,
 * only that the resource is closed correctly.
 * <p>
 * This is useful in instances where functionality is shared across both a client and server but have slightly different
 * implementation details.
 */
public interface NativeHandle extends NativeResource {
  /**
   * Returns a pointer to the native resource.
   */
  long get();

  /**
   * Drop the native resource.
   */
  void drop();
}
