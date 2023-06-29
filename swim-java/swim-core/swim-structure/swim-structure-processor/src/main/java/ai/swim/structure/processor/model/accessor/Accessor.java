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

package ai.swim.structure.processor.model.accessor;

import com.squareup.javapoet.CodeBlock;

/**
 * An abstraction over field access operations. Either direct field access or via setters and getters.
 */
public abstract class Accessor {

  /**
   * Emits a set operation on the field.
   *
   * @param builder  to emit into.
   * @param instance the class instance.
   * @param arg      the object to set the value to.
   */
  public abstract void writeSet(CodeBlock.Builder builder, String instance, Object arg);

  /**
   * Emits a get operation on the field.
   *
   * @param builder  to emit into.
   * @param instance the class instance.
   */
  public abstract void writeGet(CodeBlock.Builder builder, String instance);

  @Override
  public abstract String toString();
}
