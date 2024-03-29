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

import ai.swim.structure.annotations.FieldKind;

/**
 * Recon field manifest validation class.
 */
public class Manifest {
  boolean hasHeaderBody;
  boolean hasBody;

  /**
   * Validates {@code kind} against the current manifest.
   *
   * @throws InvalidModelException if pushing {@code kind} violates the manifest.
   */
  public void validate(FieldKind kind) {
    switch (kind) {
      case Body:
        if (hasBody) {
          throw new InvalidModelException("At most one field can replace the body.");
        }
        hasBody = true;
        break;
      case HeaderBody:
        if (hasHeaderBody) {
          throw new InvalidModelException("At most one field can replace the tag body.");
        }
        hasHeaderBody = true;
        break;
      default:
        break;
    }
  }
}
