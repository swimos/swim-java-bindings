package ai.swim.structure.processor.schema;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PartitionedFields {
  public HeaderSet headerSet;
  public Body body;

  public PartitionedFields(HeaderSet headerSet, Body body) {
    this.headerSet = headerSet;
    this.body = body;
  }

  public static PartitionedFields buildFrom(List<FieldModel> fields) {
    HeaderSet headerSet = new HeaderSet();
    Body body = new Body();

    for (FieldModel field : fields) {
      switch (field.getFieldKind()) {
        case Body:
          List<FieldModel> newHeaderFields = body.replace(field);
          headerSet.addHeaderFields(newHeaderFields);
          break;
        case Header:
          headerSet.addHeaderField(field);
          break;
        case HeaderBody:
          if (!headerSet.hasTagBody()) {
            headerSet.setTagBody(field);
          }
          break;
        case Attr:
          headerSet.addAttribute(field);
          break;
        case Slot:
          if (!body.isReplaced()) {
            body.addField(field);
          } else {
            headerSet.addHeaderField(field);
          }
          break;
      }
    }

    return new PartitionedFields(headerSet, body);
  }

  public int count() {
    return headerSet.count() + body.count();
  }

  public List<FieldModel> flatten() {
    List<FieldModel> fieldModels = this.headerSet.flatten();
    fieldModels.addAll(this.body.getFields());
    return fieldModels;
  }

  public boolean hasHeaderFields() {
    return !this.headerSet.headerFields.isEmpty() || this.headerSet.hasTagBody();
  }

  public List<FieldDiscriminate> discriminate() {
    List<FieldDiscriminate> discriminates = new ArrayList<>();

    FieldModel tagName = this.headerSet.tagName;
    if (tagName != null) {
      discriminates.add(FieldDiscriminate.tag(tagName));
    }

    if (headerSet.hasTagBody() || !headerSet.headerFields.isEmpty()) {
      discriminates.add(FieldDiscriminate.header(headerSet.tagBody, headerSet.headerFields));
    }

    discriminates.addAll(headerSet.attributes.stream().map(FieldDiscriminate::attribute).collect(Collectors.toList()));

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
        "headerFields=" + headerSet +
        ", body=" + body +
        '}';
  }

  public boolean isHeader(FieldModel model) {
    return headerSet.isHeader(model);
  }

}
