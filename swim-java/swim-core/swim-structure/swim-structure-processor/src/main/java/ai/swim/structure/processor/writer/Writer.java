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

import ai.swim.structure.processor.model.ClassLikeModel;
import ai.swim.structure.processor.model.InterfaceModel;
import java.io.IOException;

/**
 * An interface for specifying how a model should be written and transformed into another type.
 * <p>
 * Implementations accept models, transform them into a target type, and write them to the {@link javax.annotation.processing.ProcessingEnvironment}'s
 * {@link javax.annotation.processing.Filer}.
 */
public interface Writer {
  /**
   * Write a {@link ClassLikeModel}.
   */
  void writeClass(ClassLikeModel model) throws IOException;

  /**
   * Write a {@link InterfaceModel}.
   */
  void writeInterface(InterfaceModel model) throws IOException;
}
