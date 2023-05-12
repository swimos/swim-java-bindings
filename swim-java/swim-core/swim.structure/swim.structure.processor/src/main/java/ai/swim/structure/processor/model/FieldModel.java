package ai.swim.structure.processor.model;

import ai.swim.structure.annotations.AutoForm;
import ai.swim.structure.annotations.FieldKind;
import ai.swim.structure.processor.model.accessor.Accessor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.*;
import javax.lang.model.util.Types;
import java.util.Collections;
import java.util.List;

/**
 * A field and it's associated model.
 */
public class FieldModel {
  private final Accessor accessor;
  private final Model model;
  private final VariableElement element;
  private final FieldKind fieldKind;

  public FieldModel(Accessor accessor, Model model, VariableElement element, FieldKind fieldKind) {
    this.accessor = accessor;
    this.model = model;
    this.element = element;
    this.fieldKind = fieldKind;
  }

  @Override
  public String toString() {
    return "FieldModel{" +
            "accessor=" + accessor +
            ", model=" + model +
            ", element=" + element +
            ", fieldKind=" + fieldKind +
            '}';
  }

  public String propertyName() {
    AutoForm.Name name = this.element.getAnnotation(AutoForm.Name.class);
    if (name != null) {
      return name.value();
    } else {
      return this.element.getSimpleName().toString();
    }
  }

  public TypeMirror type() {
    return model.getType();
  }

  public Accessor getAccessor() {
    return accessor;
  }

  public boolean isOptional() {
    return this.element.getAnnotation(AutoForm.Optional.class) != null;
  }

  public boolean isIgnored() {
    return this.element.getAnnotation(AutoForm.Ignore.class) != null;
  }

  public FieldKind getFieldKind() {
    return this.fieldKind;
  }

  public Name getName() {
    return this.element.getSimpleName();
  }

  public TypeMirror boxedType(ProcessingEnvironment environment) {
    Types typeUtils = environment.getTypeUtils();
    TypeMirror modelType = element.asType();

    if (modelType.getKind().isPrimitive()) {
      TypeElement boxedClass = typeUtils.boxedClass((PrimitiveType) modelType);
      modelType = boxedClass.asType();
    }

    return modelType;
  }

  public boolean isParameterised() {
    TypeMirror type = model.type;
    TypeKind typeKind = type.getKind();

    switch (typeKind) {
      case TYPEVAR:
        return true;
      case DECLARED:
        DeclaredType declaredType = (DeclaredType) type;

        return declaredType.getTypeArguments().stream().anyMatch(ty -> {
          switch (ty.getKind()) {
            case DECLARED:
              DeclaredType typeVar = (DeclaredType) ty;
              return !typeVar.getTypeArguments().isEmpty();
            case TYPEVAR:
              return true;
            case WILDCARD:
              WildcardType wildcardType = (WildcardType) ty;
              return wildcardType.getExtendsBound() != null || wildcardType.getSuperBound() != null;
            default:
              throw new AssertionError("Unhandled type kind: " + ty.getKind());
          }
        });
      default:
        return false;
    }
  }

  public VariableElement getElement() {
    return element;
  }

  public List<? extends TypeMirror> typeParameters() {
    TypeMirror ty = element.asType();
    switch (ty.getKind()) {
      case DECLARED:
        DeclaredType typeVar = (DeclaredType) ty;
        return typeVar.getTypeArguments();
      case WILDCARD:
      case TYPEVAR:
        return List.of(ty);
      default:
        return Collections.emptyList();
    }
  }

  public Model getModel() {
    return model;
  }

  public InitializedType instantiate(TypeInitializer initializer, boolean inConstructor) {
    return this.model.instantiate(initializer, inConstructor);
  }

  public Object defaultValue() {
    return model.defaultValue();
  }

}
