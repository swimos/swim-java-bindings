package ai.swim.structure.processor.schema;

import ai.swim.structure.processor.model.FieldModel;

import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Description of how fields should be written into the attributes of the record.
 */
public class HeaderSet {
  /**
   * Fields that should be promoted to the body of the tag after the `tagBody` field, if it exists.
   */
  public final List<FieldModel> headerFields;
  /**
   * Fields that should be promoted to an attribute.
   */
  public final List<FieldModel> attributes;
  /**
   * A field that should be used to replaced the name of the tag attribute.
   */
  public FieldModel tagName;
  /**
   * A field that should be promoted to the body of the tag.
   */
  public FieldModel tagBody;

  public HeaderSet() {
    this.headerFields = new ArrayList<>();
    this.attributes = new ArrayList<>();
  }

  /**
   * Returns whether this header set has a tag body.
   */
  public boolean hasTagBody() {
    return this.tagBody != null;
  }

  /**
   * Returns whether this header set has a tag name.
   */
  public boolean hasTagName() {
    return this.tagName != null;
  }

  /**
   * Inserts a header field.
   */
  public void addHeaderField(FieldModel field) {
    this.headerFields.add(field);
  }

  /**
   * Inserts an attribute.
   */
  public void addAttribute(FieldModel field) {
    this.attributes.add(field);
  }

  /**
   * Returns the number of header fields and attributes.
   */
  public int count() {
    return headerFields.size() + attributes.size();
  }

  /**
   * Sets the tag body.
   */
  public void setTagBody(FieldModel tagBody) {
    this.tagBody = tagBody;
  }

  /**
   * Extends the header fields with {@code newHeaderFields}.
   */
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

  /**
   * Returns all the type parameters that this header set contains.
   */
  public HashSet<TypeMirror> typeParameters() {
    LinkedHashSet<TypeMirror> typeParameters = new LinkedHashSet<>();

    List<FieldModel> fieldSet = new ArrayList<>(headerFields);
    if (tagBody != null) {
      fieldSet.add(tagBody);
    }

    for (FieldModel headerField : fieldSet) {
      VariableElement element = headerField.getElement();
      TypeKind typeKind = element.asType().getKind();

      if (typeKind == TypeKind.DECLARED) {
        DeclaredType declaredType = (DeclaredType) element.asType();
        typeParameters.addAll(declaredType.getTypeArguments());
      } else if (typeKind == TypeKind.TYPEVAR) {
        TypeVariable typeVariable = (TypeVariable) element.asType();
        typeParameters.add(typeVariable);
      }
    }

    return typeParameters;
  }

  /**
   * Returns whether {@code model} is a header.
   */
  public boolean isHeader(FieldModel model) {
    return headerFields.contains(model);
  }
}
