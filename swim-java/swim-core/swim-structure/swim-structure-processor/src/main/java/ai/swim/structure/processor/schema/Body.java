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

package ai.swim.structure.processor.schema;

import ai.swim.structure.processor.model.FieldModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Fields that should be written into the body of a record.
 */
public class Body {
  /**
   * Whether the body of the record has been replaced.
   */
  private boolean isReplaced;
  /**
   * Fields in the body.
   */
  private List<FieldModel> fields;

  public Body() {
    this.isReplaced = false;
    this.fields = new ArrayList<>();
  }

  /**
   * Returns whether the body has been replaced.
   */
  public boolean isReplaced() {
    return isReplaced;
  }

  /**
   * Inserts a new field into the body.
   */
  public void addField(FieldModel model) {
    if (this.isReplaced && this.fields.size() == 1) {
      throw new AssertionError();
    }

    this.fields.add(model);
  }

  /**
   * Returns the number of fields in the body.
   */
  public int count() {
    return fields.size();
  }

  /**
   * Replaces the body of the record with {@code field}.
   *
   * @return the fields that were in the body previously.
   */
  public List<FieldModel> replace(FieldModel field) {
    if (this.isReplaced) {
      throw new AssertionError();
    }

    this.isReplaced = true;
    List<FieldModel> fields = this.fields;
    this.fields = new ArrayList<>(Collections.singleton(field));

    return fields;
  }

  /**
   * Returns a list of the fields that are in the body.
   */
  public List<FieldModel> getFields() {
    return fields;
  }

  @Override
  public String toString() {
    return "Body{" +
        "isReplaced=" + isReplaced +
        ", fields=" + fields +
        '}';
  }
}
