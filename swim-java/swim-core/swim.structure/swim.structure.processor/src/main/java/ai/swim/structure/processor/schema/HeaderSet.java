package ai.swim.structure.processor.schema;

import java.util.ArrayList;
import java.util.List;

public class HeaderSet {
  public FieldModel tagName;
  public FieldModel tagBody;
  public final List<FieldModel> headerFields;
  public final List<FieldModel> attributes;

  public HeaderSet() {
    this.headerFields = new ArrayList<>();
    this.attributes = new ArrayList<>();
  }

  public boolean hasTagBody() {
    return this.tagBody != null;
  }

  public boolean hasTagName() {
    return this.tagName != null;
  }

  public void setTagBody(FieldModel tagBody) {
    this.tagBody = tagBody;
  }

  public void addHeaderField(FieldModel field) {
    this.headerFields.add(field);
  }

  public void addAttribute(FieldModel field) {
    this.attributes.add(field);
  }

  public int count() {
    return headerFields.size() + attributes.size();
  }

  public FieldModel getTagBody() {
    return tagBody;
  }

  public List<FieldModel> getHeaderFields() {
    return headerFields;
  }

  public List<FieldModel> getAttributes() {
    return attributes;
  }

  public List<FieldModel> flatten() {
    ArrayList<FieldModel> fieldModels = new ArrayList<>(this.headerFields);
    fieldModels.addAll(attributes);
    return fieldModels;
  }

  public void addHeaderFields(List<FieldModel> newHeaderFields) {
    this.headerFields.addAll(newHeaderFields);
  }

  @Override
  public String toString() {
    return "HeaderFields{" +
        "tagName=" + tagName +
        ", tagBody=" + tagBody +
        ", headerFields=" + headerFields +
        ", attributes=" + attributes +
        '}';
  }
}
