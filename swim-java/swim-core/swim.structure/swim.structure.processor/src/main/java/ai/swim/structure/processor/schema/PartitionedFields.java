package ai.swim.structure.processor.schema;

import ai.swim.structure.processor.model.FieldModel;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Description of how the fields of a type are written into a record.
 */
public class PartitionedFields {
  public final HeaderSet headerSet;
  public final Body body;

  public PartitionedFields(HeaderSet headerSet, Body body) {
    this.headerSet = headerSet;
    this.body = body;
  }

  /**
   * Partitions {@code fields} according to their field kind.
   */
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

  /**
   * Returns the total number of header and body fields.
   */
  public int count() {
    return headerSet.count() + body.count();
  }

  /**
   * Returns whether there are any header fields.
   */
  public boolean hasHeaderFields() {
    return !this.headerSet.headerFields.isEmpty() || this.headerSet.hasTagBody();
  }

  /**
   * Discriminates against all of the fields by their type; tag, header, attribute, item or body.
   *
   * @return
   */
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

  /**
   * Returns whether {@code model} is a header.
   */
  public boolean isHeader(FieldModel model) {
    return headerSet.isHeader(model);
  }

}
