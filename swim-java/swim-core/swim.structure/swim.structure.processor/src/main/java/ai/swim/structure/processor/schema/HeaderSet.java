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

public class HeaderSet {
  public final List<FieldModel> headerFields;
  public final List<FieldModel> attributes;
  public FieldModel tagName;
  public FieldModel tagBody;

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

  public void addHeaderField(FieldModel field) {
    this.headerFields.add(field);
  }

  public void addAttribute(FieldModel field) {
    this.attributes.add(field);
  }

  public int count() {
    return headerFields.size() + attributes.size();
  }

  public void setTagBody(FieldModel tagBody) {
    this.tagBody = tagBody;
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

  public boolean isHeader(FieldModel model) {
    return headerFields.contains(model);
  }
}
