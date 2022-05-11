package ai.swim.structure.processor.schema;

import java.util.ArrayList;
import java.util.List;

public class Body {
  private boolean isReplaced;
  private final List<FieldModel> fields;

  public Body() {
    this.isReplaced = false;
    this.fields = new ArrayList<>();
  }

  public boolean isReplaced() {
    return isReplaced;
  }

  public void addField(FieldModel model) {
    this.fields.add(model);
  }

  public int count() {
    return fields.size();
  }

  public void setReplaced() {
    this.isReplaced = true;
  }

  public List<FieldModel> getFields() {
    return fields;
  }
}
