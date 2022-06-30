package ai.swim.structure.processor.schema;

import java.util.List;

public abstract class FieldDiscriminate {

  public boolean isTag() {
    return false;
  }

  public boolean isHeader() {
    return false;
  }

  public boolean isAttribute() {
    return false;
  }

  public boolean isItem() {
    return false;
  }

  public boolean isBody() {
    return false;
  }

  public static FieldDiscriminate tag(FieldModel field) {
    return new SingleField(field) {
      @Override
      public boolean isTag() {
        return true;
      }
    };
  }

  public static FieldDiscriminate header(FieldModel tagBody, List<FieldModel> fields) {
    return new HeaderFields(tagBody, fields) {
      @Override
      public boolean isHeader() {
        return true;
      }
    };
  }

  public static FieldDiscriminate attribute(FieldModel field) {
    return new SingleField(field) {
      @Override
      public boolean isAttribute() {
        return true;
      }
    };
  }

  public static FieldDiscriminate item(FieldModel field) {
    return new SingleField(field) {
      @Override
      public boolean isItem() {
        return true;
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
