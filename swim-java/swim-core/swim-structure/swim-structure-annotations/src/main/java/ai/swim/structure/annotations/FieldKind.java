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

package ai.swim.structure.annotations;

public enum FieldKind {
  /**
   * The field should be used to form the entire body of the structure, all other fields that are marked as slots will
   * be promoted to headers. At most one field may be marked with this.
   */
  Body,
  /**
   * The field should be written as a slot in the tag attribute.
   */
  Header,
  /**
   * The field should be moved into the body of the tag attribute (unlabelled). If there are no header fields it will
   * form the entire body of the tag, otherwise it will be the first item of the tag body. At most one field may be
   * marked with this.
   */
  HeaderBody,
  /**
   * The field should be written as an attribute in the structure.
   */
  Attr,
  /**
   * The field should be written as a slot in the main body or the header if another field is marked as `body`. A field
   * marked with no positional attribute will default to being written as a slot in the structure.
   */
  Slot
}