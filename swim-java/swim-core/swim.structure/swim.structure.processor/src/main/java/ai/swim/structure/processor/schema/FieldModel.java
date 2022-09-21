package ai.swim.structure.processor.schema;

import ai.swim.structure.annotations.AutoForm;
import ai.swim.structure.annotations.FieldKind;
import ai.swim.structure.processor.inspect.accessor.Accessor;
import ai.swim.structure.processor.recognizer.context.ScopedContext;
import ai.swim.structure.processor.recognizer.models.RecognizerModel;
import com.squareup.javapoet.CodeBlock;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.Types;

public class FieldModel {
  private final Accessor accessor;
  private final RecognizerModel recognizer;
  private final VariableElement element;
  private final FieldKind fieldKind;

  public FieldModel(Accessor accessor, RecognizerModel recognizer, VariableElement element, FieldKind fieldKind) {
    this.accessor = accessor;
    this.recognizer = recognizer;
    this.element = element;
    this.fieldKind = fieldKind;
  }

  @Override
  public String toString() {
    return "FieldModel{" +
        "accessor=" + accessor +
        ", recognizer=" + recognizer +
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

  public TypeMirror type(ProcessingEnvironment environment) {
    TypeMirror type = this.recognizer.type(environment);
    if (type == null) {
      return this.element.asType();
    } else {
      return type;
    }
  }

  public CodeBlock initializer(ScopedContext context, boolean inConstructor) {
    return this.recognizer.initializer(context, inConstructor);
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

  public Object defaultValue() {
    return this.recognizer.defaultValue();
  }

  public FieldKind getFieldKind() {
    return this.fieldKind;
  }

  public Name getName() {
    return this.element.getSimpleName();
  }

  public TypeMirror boxedType(ProcessingEnvironment environment) {
    Types typeUtils = environment.getTypeUtils();
    TypeMirror recognizerType = type(environment);

    if (recognizerType.getKind().isPrimitive()) {
      TypeElement boxedClass = typeUtils.boxedClass((PrimitiveType) recognizer.type(environment));
      recognizerType = boxedClass.asType();
    }

    return recognizerType;
  }

  public boolean isParameterised(ScopedContext context) {
    TypeMirror type = type(context.getProcessingEnvironment());
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
        throw new AssertionError("Unexpected type kind when processing generic parameters: " + typeKind + " in " + context.getRoot());
    }
  }

  public boolean isPublic() {
    for (Modifier modifier : this.element.getModifiers()) {
      switch (modifier) {
        case PUBLIC:
          return true;
        case PROTECTED:
        case PRIVATE:
          return false;
      }
    }

    return false;
  }

  public VariableElement getElement() {
    return element;
  }
}
