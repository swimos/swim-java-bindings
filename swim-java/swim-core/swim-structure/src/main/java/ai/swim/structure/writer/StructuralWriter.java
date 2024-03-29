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

package ai.swim.structure.writer;

/**
 * Interface for defining how to write {@code T} into another type.
 *
 * @param <T> the target type.
 */
public interface StructuralWriter<T> extends PrimitiveWriter<T> {

  /**
   * Returns a header writer.
   *
   * @param numAttrs a hint as to the number of attributes the header will contain.
   * @return a header writer.
   */
  HeaderWriter<T> record(int numAttrs);
}
