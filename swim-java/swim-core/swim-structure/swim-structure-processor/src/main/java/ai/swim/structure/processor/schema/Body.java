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
