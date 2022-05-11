package ai.swim.structure.processor.schema;

import java.util.ArrayList;
import java.util.List;

public class HeaderFields {
  private FieldModel tagBody;
  private final List<FieldModel> headerFields;
  private final List<FieldModel> attributes;

  public HeaderFields() {
    this.headerFields = new ArrayList<>();
    this.attributes = new ArrayList<>();
  }

  public boolean hasTagBody() {
    return this.tagBody != null;
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

  public  List<FieldModel> flatten() {
    ArrayList<FieldModel> fieldModels = new ArrayList<>(this.headerFields);
    fieldModels.addAll(attributes);
    return fieldModels;
  }
}
