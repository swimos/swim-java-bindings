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

package ai.swim.structure.processor.writer;

import com.squareup.javapoet.CodeBlock;

/**
 * An abstract class that implementors may use for interfacing with {@link CodeBlock}'s. This serves as a shorthand
 * operator as opposed to pushing individual {@link CodeBlock}'s into one another as the {@link CodeBlock} will call
 * {@code toString} on this emitter.
 */
public abstract class Emitter {
  @Override
  public abstract String toString();
}
