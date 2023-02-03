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

/**
 * Marker interface for objects that contain pointers to memory allocated by a native runtime. Objects that implement
 * this may register a destructor that will be run when there are no more phantom references to this object.
 * <p>
 * Objects which implement this interface will generally be wrappers around functionality that is implemented by a
 * native interface and *should* perform memory management automatically but if this is not possible or there is a
 * memory leak then the destructor will automatically run and reclaim the memory by invoking the provided callback. Any
 * implementations should perform checks against double freeing; a simple atomic boolean which is negated when the
 * object is correctly deallocated and checked in the destructor should suffice.
 */
public interface NativeResource {

}
