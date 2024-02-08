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

package ai.swim.structure.processor.model;

import com.squareup.javapoet.CodeBlock;
import javax.lang.model.type.TypeMirror;

/**
 * An initialized type built using a {@link TypeInitializer}.
 * <p>
 * These types are built from a model and provide a transformation from a model to either a recognizer or a writer.
 *
 * <h3>Example representation</h3>
 * If a model represented an Integer and the Type Initializer was for the writer then an initialized type may produce a
 * type mirror of {@code Writable<Integer>} and an initializer of {@code ScalarWriters.INTEGER}
 */
public class InitializedType {
  private final TypeMirror mirror;
  private final CodeBlock initializer;

  public InitializedType(TypeMirror mirror, CodeBlock initializer) {
    this.mirror = mirror;
    this.initializer = initializer;
  }

  @Override
  public String toString() {
    return initializer.toString();
  }

  public CodeBlock getInitializer() {
    return initializer;
  }

  public TypeMirror getMirror() {
    return mirror;
  }
}
