package ai.swim.structure.processor.schema;

import java.util.List;

public class PartitionedFields {
  public HeaderFields headerFields;
  public Body body;

  public PartitionedFields(HeaderFields headerFields, Body body) {
    this.headerFields = headerFields;
    this.body = body;
  }

  public static PartitionedFields buildFrom(List<FieldModel> fields) {
    HeaderFields headerFields = new HeaderFields();
    Body body = new Body();

    for (FieldModel field : fields) {
      switch (field.getFieldKind()) {
        case Body:
          if (!headerFields.hasTagBody()) {
            headerFields.addHeaderField(field);
            body.setReplaced();
          }
          break;
        case Header:
          headerFields.addHeaderField(field);
          break;
        case HeaderBody:
          if (!body.isReplaced()) {
            headerFields.setTagBody(field);
          }
          break;
        case Attr:
          headerFields.addAttribute(field);
          break;
        case Slot:
          if (headerFields.hasTagBody()) {
            headerFields.addHeaderField(field);
          } else {
            body.addField(field);
          }
          break;
      }
    }

    return new PartitionedFields(headerFields, body);
  }

  public int count() {
    return headerFields.count() + body.count();
  }

  public List<FieldModel> flatten() {
    List<FieldModel> fieldModels = this.headerFields.flatten();
    fieldModels.addAll(this.body.getFields());
    return fieldModels;
  }

  public boolean hasHeaderFields() {
    return !this.headerFields.headerFields.isEmpty();
  }
}
