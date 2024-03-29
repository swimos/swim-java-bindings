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
import java.util.List;

/**
 * A field discrimination for its placement in a record.
 */
public abstract class FieldDiscriminant {

  public static FieldDiscriminant tag(FieldModel field) {
    return new SingleField(field) {
      @Override
      public boolean isTag() {
        return true;
      }
    };
  }

  public static FieldDiscriminant header(FieldModel tagBody, List<FieldModel> fields) {
    return new HeaderFields(tagBody, fields);
  }

  public static FieldDiscriminant attribute(FieldModel field) {
    return new SingleField(field) {
      @Override
      public FieldModel getSingleField() {
        return getField();
      }
    };
  }

  public static FieldDiscriminant item(FieldModel field) {
    return new SingleField(field) {
      @Override
      public FieldModel getSingleField() {
        return getField();
      }
    };
  }

  public static FieldDiscriminant body(FieldModel field) {
    return new SingleField(field) {
      @Override
      public boolean isBody() {
        return true;
      }
    };
  }

  /**
   * Returns whether this field contains the tag.
   */
  public boolean isTag() {
    return false;
  }

  /**
   * Returns whether this field is in the header of the record.
   */
  public boolean isHeader() {
    return false;
  }

  /**
   * Returns whether this field is in the body of the record.
   */
  public boolean isBody() {
    return false;
  }

  /**
   * Returns the value of this discriminate if this field is a tag, attribute, item or delegate body.
   */
  public FieldModel getSingleField() {
    throw new UnsupportedOperationException();
  }

  public static class SingleField extends FieldDiscriminant {
    private final FieldModel field;

    public SingleField(FieldModel field) {
      this.field = field;
    }

    public FieldModel getField() {
      return field;
    }

    @Override
    public String toString() {
      return "SingleField{" +
          "field=" + field +
          '}';
    }

    @Override
    public FieldModel getSingleField() {
      return field;
    }
  }

  public static class HeaderFields extends FieldDiscriminant {
    private final List<FieldModel> fields;
    private final FieldModel tagBody;

    public HeaderFields(FieldModel tagBody, List<FieldModel> fields) {
      this.tagBody = tagBody;
      this.fields = fields;
    }

    @Override
    public boolean isHeader() {
      return true;
    }

    public FieldModel getTagBody() {
      return tagBody;
    }

    public List<FieldModel> getFields() {
      return fields;
    }

    @Override
    public String toString() {
      return "HeaderFields{" +
          "tagBody=" + tagBody +
          "fields=" + fields +
          '}';
    }
  }

}
