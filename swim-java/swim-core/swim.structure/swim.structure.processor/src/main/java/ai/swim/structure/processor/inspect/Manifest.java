package ai.swim.structure.processor.inspect;

import ai.swim.structure.processor.context.ScopedMessager;

public class Manifest {
  boolean hasHeaderBody;
  boolean hasBody;
  boolean hasTag;//todo

  public boolean validate(FieldView field, ScopedMessager messager) {
    switch (field.getFieldKind()) {
      case Body:
        if (hasBody) {
          messager.error("At most one field can replace the body.");
          return false;
        }
        hasBody = true;
        break;
      case HeaderBody:
        if (hasHeaderBody) {
          messager.error("At most one field can replace the tag body.");
          return false;
        }
        hasHeaderBody = true;
        break;
      default:
        break;
    }
    return true;
  }
}
