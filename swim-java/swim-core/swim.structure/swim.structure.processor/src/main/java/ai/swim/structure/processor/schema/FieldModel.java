package ai.swim.structure.processor.schema;

import ai.swim.structure.annotations.FieldKind;
import ai.swim.structure.processor.context.ScopedContext;
import ai.swim.structure.processor.inspect.FieldView;
import ai.swim.structure.processor.inspect.accessor.Accessor;
import ai.swim.structure.processor.recognizer.RecognizerModel;
import com.squareup.javapoet.CodeBlock;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.*;
import javax.lang.model.util.Types;

public class FieldModel {
  private final Accessor accessor;
  private final RecognizerModel recognizer;
  private final FieldView fieldView;

  public FieldModel(Accessor accessor, RecognizerModel recognizer, FieldView fieldView) {
    this.accessor = accessor;
    this.recognizer = recognizer;
    this.fieldView = fieldView;
  }

  @Override
  public String toString() {
    return "FieldModel{" +
        "accessor=" + accessor +
        ", recognizer=" + recognizer +
        ", fieldView=" + fieldView +
        '}';
  }

  public String fieldName() {
    return this.fieldView.getName().toString();
  }

  public String propertyName() {
    return this.fieldView.propertyName();
  }

  public TypeMirror type(ProcessingEnvironment environment) {
    TypeMirror type = this.recognizer.type(environment);
    if (type == null) {
      return this.fieldView.getElement().asType();
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
    return this.fieldView.isOptional();
  }

  public Object defaultValue() {
    return this.recognizer.defaultValue();
  }

  public FieldKind getFieldKind() {
    return fieldView.getFieldKind();
  }

  public Name getName() {
    return this.fieldView.getName();
  }

  public FieldView getFieldView() {
    return this.fieldView;
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
}
