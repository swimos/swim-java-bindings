package ai.swim.structure.processor.schema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// todo refactor into Either<One, Many>
public class Body {
  private boolean isReplaced;
  private List<FieldModel> fields;

  public Body() {
    this.isReplaced = false;
    this.fields = new ArrayList<>();
  }

  public boolean isReplaced() {
    return isReplaced;
  }

  public void addField(FieldModel model) {
    if (this.isReplaced && this.fields.size() == 1) {
      throw new AssertionError();
    }

    this.fields.add(model);
  }

  public int count() {
    return fields.size();
  }

  public List<FieldModel> replace(FieldModel field) {
    if (this.isReplaced) {
      throw new AssertionError();
    }

    this.isReplaced = true;
    List<FieldModel> fields = this.fields;
    this.fields = new ArrayList<>(Collections.singleton(field));

    return fields;
  }

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
