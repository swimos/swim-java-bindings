package ai.swim.structure.processor.schema;

import ai.swim.structure.processor.writer.FieldDiscriminate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
          List<FieldModel> newHeaderFields = body.replace(field);
          headerFields.addHeaderFields(newHeaderFields);
          break;
        case Header:
          headerFields.addHeaderField(field);
          break;
        case HeaderBody:
          if (!headerFields.hasTagBody()) {
            headerFields.setTagBody(field);
          }
          break;
        case Attr:
          headerFields.addAttribute(field);
          break;
        case Slot:
          if (!body.isReplaced()) {
            body.addField(field);
          } else {
            headerFields.addHeaderField(field);
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

  public List<FieldDiscriminate> discriminate() {
    List<FieldDiscriminate> discriminates = new ArrayList<>();

    FieldModel tagName = this.headerFields.tagName;
    if (tagName != null) {
      discriminates.add(FieldDiscriminate.tag(tagName));
    }

    if (headerFields.hasTagBody() || !headerFields.headerFields.isEmpty()) {
      discriminates.add(FieldDiscriminate.header(headerFields.tagBody, headerFields.headerFields));
    }

    discriminates.addAll(headerFields.attributes.stream().map(FieldDiscriminate::attribute).collect(Collectors.toList()));

    List<FieldModel> bodyFields = body.getFields();

    if (body.isReplaced()) {
      discriminates.add(FieldDiscriminate.body(bodyFields.get(0)));
    } else {
      discriminates.addAll(bodyFields.stream().map(FieldDiscriminate::item).collect(Collectors.toList()));
    }

    return discriminates;
  }

  @Override
  public String toString() {
    return "PartitionedFields{" +
        "headerFields=" + headerFields +
        ", body=" + body +
        '}';
  }
}
