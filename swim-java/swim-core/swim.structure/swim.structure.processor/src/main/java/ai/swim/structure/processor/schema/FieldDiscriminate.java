package ai.swim.structure.processor.schema;

import ai.swim.structure.processor.model.FieldModel;

import java.util.List;

/**
 * A field discrimination for its placement in a record.
 */
public abstract class FieldDiscriminate {

  public static FieldDiscriminate tag(FieldModel field) {
    return new SingleField(field) {
      @Override
      public boolean isTag() {
        return true;
      }
    };
  }

  public static FieldDiscriminate header(FieldModel tagBody, List<FieldModel> fields) {
    return new HeaderFields(tagBody, fields);
  }

  public static FieldDiscriminate attribute(FieldModel field) {
    return new SingleField(field) {
      @Override
      public FieldModel getSingleField() {
        return getField();
      }
    };
  }

  public static FieldDiscriminate item(FieldModel field) {
    return new SingleField(field) {
      @Override
      public FieldModel getSingleField() {
        return getField();
      }
    };
  }

  public static FieldDiscriminate body(FieldModel field) {
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

  public static class SingleField extends FieldDiscriminate {
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

  public static class HeaderFields extends FieldDiscriminate {
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
