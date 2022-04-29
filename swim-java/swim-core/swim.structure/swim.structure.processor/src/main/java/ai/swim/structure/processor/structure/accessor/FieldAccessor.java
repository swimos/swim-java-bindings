package ai.swim.structure.processor.structure.accessor;

import ai.swim.structure.processor.FieldView;

public class FieldAccessor extends Accessor {
  private final FieldView field;

  public FieldAccessor(FieldView field) {
    this.field = field;
  }

  @Override
  public void write(Object with, Object var) {

  }

  @Override
  public String toString() {
    return "FieldAccessor{" +
        "field=" + field +
        '}';
  }
}
