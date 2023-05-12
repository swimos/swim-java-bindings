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
  /**
   * Accessors for field access.
   */
  private final Accessor accessor;
  /**
   * The corresponding model for this field.
   */
  private final Model model;
  /**
   * The Java variable element from the originating element.
   */
  private final VariableElement element;
  /**
   * The Recon field kind.
   */
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

  /**
   * Returns a String representation of this field's property name. Either the value from {@code Name} or the variable
   * element's simple name.
   */
  public String propertyName() {
    AutoForm.Name name = this.element.getAnnotation(AutoForm.Name.class);
    if (name != null) {
      return name.value();
    } else {
      return this.element.getSimpleName().toString();
    }
  }

  /**
   * Returns the {@code TypeMirror} for the model.
   */
  public TypeMirror type() {
    return model.getType();
  }

  /**
   * Returns this field's accessor.
   */
  public Accessor getAccessor() {
    return accessor;
  }

  /**
   * Returns whether this field is optional.
   */
  public boolean isOptional() {
    return this.element.getAnnotation(AutoForm.Optional.class) != null;
  }

  /**
   * Returns whether this field is ignored.
   */
  public boolean isIgnored() {
    return this.element.getAnnotation(AutoForm.Ignore.class) != null;
  }

  /**
   * Returns the Recon field kind.
   */
  public FieldKind getFieldKind() {
    return this.fieldKind;
  }

  /**
   * Returns the variable element's simple name.
   */
  public Name getName() {
    return this.element.getSimpleName();
  }

  /**
   * Returns a boxed type mirror of this field element's type.
   */
  public TypeMirror boxedType(ProcessingEnvironment environment) {
    Types typeUtils = environment.getTypeUtils();
    TypeMirror modelType = element.asType();

    if (modelType.getKind().isPrimitive()) {
      TypeElement boxedClass = typeUtils.boxedClass((PrimitiveType) modelType);
      modelType = boxedClass.asType();
    }

    return modelType;
  }

  /**
   * Returns whether this field is parameterised.
   */
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

  /**
   * Returns this field's corresponding variable element.
   */
  public VariableElement getElement() {
    return element;
  }

  /**
   * Returns this field's type parameters, if any.
   */
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

  /**
   * Returns this field's model.
   */
  public Model getModel() {
    return model;
  }

  /**
   * Instantiates this field using the {@code TypeInitializer} and returns an initialized, mapped, type mirror and a
   * code block for writing.
   *
   * @param initializer   for the transformation.
   * @param inConstructor whether in the current context the code block will be written in a constructor.
   * @return an initialized type .
   */
  public InitializedType instantiate(TypeInitializer initializer, boolean inConstructor) {
    return this.model.instantiate(initializer, inConstructor);
  }

  /**
   * Returns this field's default value.
   */
  public Object defaultValue() {
    return model.defaultValue();
  }

}
