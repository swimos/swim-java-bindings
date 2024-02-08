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

import javax.lang.model.type.TypeMirror;

/**
 * A model visitor for initializing types. Implementations detail a contract for transposing between a model and another
 * type, such as a recognizer or a writer.
 *
 * <h3>Example representation</h3>
 * If a model represented an Integer and the Type Initializer was for the writer then an initialized type may produce a
 * type mirror of {@code Writable<Integer>} and an initializer of {@code ScalarWriters.INTEGER}
 */
public interface TypeInitializer {
  /**
   * Construct an initialized type from a core type model.
   *
   * @param model to construct.
   * @return an initialized type.
   */
  InitializedType core(CoreTypeModel model);

  /**
   * Construct an initialized type from an array library model.
   *
   * @param model         to construct
   * @param inConstructor whether, in the current context, the code block will be written to a constructor.
   * @return an initialized type.
   */
  InitializedType arrayType(ArrayLibraryModel model, boolean inConstructor);

  /**
   * Construct an initialized type from a type variable that had no bounds.
   *
   * @param type          that had no bounds. Such as {@code E}.
   * @param inConstructor whether, in the current context, the code block will be written to a constructor.
   * @return an initialized type.
   */
  InitializedType untyped(TypeMirror type, boolean inConstructor);

  /**
   * Construct an initialized type from a resolved declared type. This may be a known type model or a structural model.
   *
   * @param model         to construct
   * @param inConstructor whether, in the current context, the code block will be written to a constructor.
   * @param parameters    that this type accepts.
   * @return an initialized type.
   */
  InitializedType declared(Model model, boolean inConstructor, Model... parameters);

  /**
   * Construct an initialized type from a model that failed model resolution.
   *
   * @param model         to construct
   * @param inConstructor whether, in the current context, the code block will be written to a constructor.
   * @return an initialized type.
   * @throws InvalidModelException if it's not possible to produce a model.
   */
  InitializedType unresolved(Model model, boolean inConstructor) throws InvalidModelException;
}
